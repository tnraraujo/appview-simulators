[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$ImageNamespace,

    [Parameter()]
    [ValidateNotNullOrEmpty()]
    [string]$Tag = "latest",

    [Parameter()]
    [ValidateNotNullOrEmpty()]
    [string]$Registry = "docker.io",

    [Parameter()]
    [ValidateNotNullOrEmpty()]
    [string]$RootPath = ".",

    [Parameter()]
    [ValidateNotNullOrEmpty()]
    [string]$GradleTask = "bootJar",

    [Parameter()]
    [switch]$SkipTests,

    [Parameter()]
    [switch]$PushLatest,

    [Parameter()]
    [string]$RegistryUser,

    [Parameter()]
    [string]$RegistryToken,

    [Parameter()]
    [string[]]$Projects = @("ccm", "jarvis", "pega", "prestador"),

    [Parameter()]
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Assert-CommandExists {
    param([Parameter(Mandatory = $true)][string]$CommandName)

    if (-not (Get-Command -Name $CommandName -ErrorAction SilentlyContinue)) {
        throw "Comando '$CommandName' nao encontrado no PATH."
    }
}

function Get-FullImageRef {
    param(
        [Parameter(Mandatory = $true)][string]$Registry,
        [Parameter(Mandatory = $true)][string]$ImageName,
        [Parameter(Mandatory = $true)][string]$Tag
    )

    if ($Registry -eq "docker.io") {
        return "${ImageName}:${Tag}"
    }

    return "${Registry}/${ImageName}:${Tag}"
}

function Invoke-ExternalCommand {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter()][string[]]$Arguments = @(),
        [Parameter(Mandatory = $true)][string]$Description,
        [Parameter()][switch]$DryRun
    )

    $display = "$FilePath " + ($Arguments -join " ")
    Write-Host "==> $Description" -ForegroundColor Cyan
    Write-Host "    $display"

    if ($DryRun) {
        return
    }

    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Falha em: $Description (exit code $LASTEXITCODE)."
    }
}

try {
    $resolvedRootPath = (Resolve-Path -Path $RootPath).Path
    Assert-CommandExists -CommandName "docker"

    $allowedProjects = @("ccm", "jarvis", "pega", "prestador")
    $normalizedProjects = @(
        $Projects |
            ForEach-Object { $_ -split "," } |
            ForEach-Object { $_.Trim().ToLowerInvariant() } |
            Where-Object { $_ -ne "" } |
            Select-Object -Unique
    )

    if ($normalizedProjects.Count -eq 0) {
        throw "Nenhum projeto valido informado em -Projects."
    }

    $invalidProjects = @($normalizedProjects | Where-Object { $_ -notin $allowedProjects })
    if ($invalidProjects.Count -gt 0) {
        throw "Projetos invalidos: $($invalidProjects -join ', '). Permitidos: $($allowedProjects -join ', ')."
    }

    if (-not [string]::IsNullOrWhiteSpace($RegistryUser) -and -not [string]::IsNullOrWhiteSpace($RegistryToken)) {
        Write-Host "==> Login no registry $Registry" -ForegroundColor Cyan
        if (-not $DryRun) {
            $RegistryToken | docker login $Registry -u $RegistryUser --password-stdin | Out-Null
            if ($LASTEXITCODE -ne 0) {
                throw "Falha no docker login para '$Registry'."
            }
        }
    }
    else {
        Write-Host "==> Login nao executado: usando sessao Docker ja autenticada (docker login)." -ForegroundColor Yellow
    }

    $results = @()

    foreach ($project in $normalizedProjects) {
        $projectPath = Join-Path $resolvedRootPath $project
        $gradleWrapper = Join-Path $projectPath "gradlew.bat"
        $dockerfilePath = Join-Path $projectPath "Dockerfile"
        $imageName = "${ImageNamespace}/${project}"
        $imageRef = Get-FullImageRef -Registry $Registry -ImageName $imageName -Tag $Tag

        Write-Host ""
        Write-Host "===== Projeto: $project =====" -ForegroundColor Magenta

        try {
            if (-not (Test-Path -Path $projectPath)) {
                throw "Projeto '$project' nao encontrado em '$projectPath'."
            }

            if (-not (Test-Path -Path $gradleWrapper)) {
                throw "gradlew.bat nao encontrado em '$gradleWrapper'."
            }

            if (-not (Test-Path -Path $dockerfilePath)) {
                throw "Dockerfile nao encontrado em '$dockerfilePath'."
            }

            Push-Location $projectPath
            try {
                $gradleArgs = @("clean", $GradleTask, "--no-daemon")
                if ($SkipTests) {
                    $gradleArgs += @("-x", "test")
                }

                Invoke-ExternalCommand -FilePath $gradleWrapper -Arguments $gradleArgs -Description "Build Gradle ($project)" -DryRun:$DryRun
                Invoke-ExternalCommand -FilePath "docker" -Arguments @("build", "-t", $imageRef, "-f", $dockerfilePath, ".") -Description "Build imagem Docker ($project)" -DryRun:$DryRun
                Invoke-ExternalCommand -FilePath "docker" -Arguments @("push", $imageRef) -Description "Push imagem Docker ($project)" -DryRun:$DryRun

                if ($PushLatest -and $Tag -ne "latest") {
                    $latestRef = Get-FullImageRef -Registry $Registry -ImageName $imageName -Tag "latest"
                    Invoke-ExternalCommand -FilePath "docker" -Arguments @("tag", $imageRef, $latestRef) -Description "Tag latest ($project)" -DryRun:$DryRun
                    Invoke-ExternalCommand -FilePath "docker" -Arguments @("push", $latestRef) -Description "Push latest ($project)" -DryRun:$DryRun
                }
            }
            finally {
                Pop-Location
            }

            $results += [PSCustomObject]@{
                Projeto = $project
                Status = "SUCESSO"
                Imagem = $imageRef
                Erro = ""
            }
        }
        catch {
            $results += [PSCustomObject]@{
                Projeto = $project
                Status = "FALHA"
                Imagem = $imageRef
                Erro = $_.Exception.Message
            }

            Write-Host "Erro no projeto ${project}: $($_.Exception.Message)" -ForegroundColor Red
        }
    }

    Write-Host ""
    Write-Host "===== Resumo =====" -ForegroundColor Green
    $results | Format-Table -AutoSize | Out-String | Write-Host

    $failed = @($results | Where-Object { $_.Status -eq "FALHA" })
    if ($failed.Count -gt 0) {
        exit 1
    }

    exit 0
}
catch {
    Write-Error $_.Exception.Message
    exit 1
}


