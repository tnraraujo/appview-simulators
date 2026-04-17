[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$ImageName,

    [Parameter()]
    [ValidateNotNullOrEmpty()]
    [string]$Tag = "latest",

    [Parameter()]
    [ValidateNotNullOrEmpty()]
    [string]$Registry = "docker.io",

    [Parameter()]
    [ValidateNotNullOrEmpty()]
    [string]$ProjectPath = ".",

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
    $resolvedProjectPath = (Resolve-Path -Path $ProjectPath).Path
    $gradleWrapper = Join-Path $resolvedProjectPath "gradlew.bat"
    $dockerfilePath = Join-Path $resolvedProjectPath "Dockerfile"

    if (-not (Test-Path -Path $gradleWrapper)) {
        throw "Nao encontrei o wrapper do Gradle em '$gradleWrapper'."
    }

    if (-not (Test-Path -Path $dockerfilePath)) {
        throw "Nao encontrei o Dockerfile em '$dockerfilePath'."
    }

    Assert-CommandExists -CommandName "docker"

    $imageRef = Get-FullImageRef -Registry $Registry -ImageName $ImageName -Tag $Tag

    Push-Location $resolvedProjectPath
    try {
        $gradleArgs = @("clean", $GradleTask, "--no-daemon")
        if ($SkipTests) {
            $gradleArgs += @("-x", "test")
        }

        Invoke-ExternalCommand -FilePath $gradleWrapper -Arguments $gradleArgs -Description "Build Gradle ($GradleTask)" -DryRun:$DryRun

        $dockerBuildArgs = @("build", "-t", $imageRef, "-f", $dockerfilePath, ".")
        Invoke-ExternalCommand -FilePath "docker" -Arguments $dockerBuildArgs -Description "Build da imagem Docker" -DryRun:$DryRun

        if (-not [string]::IsNullOrWhiteSpace($RegistryUser) -and -not [string]::IsNullOrWhiteSpace($RegistryToken)) {
            Write-Host "==> Login no registry $Registry com credenciais informadas" -ForegroundColor Cyan
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

        Invoke-ExternalCommand -FilePath "docker" -Arguments @("push", $imageRef) -Description "Push da imagem $imageRef" -DryRun:$DryRun

        if ($PushLatest -and $Tag -ne "latest") {
            $latestRef = Get-FullImageRef -Registry $Registry -ImageName $ImageName -Tag "latest"
            Invoke-ExternalCommand -FilePath "docker" -Arguments @("tag", $imageRef, $latestRef) -Description "Tag adicional latest" -DryRun:$DryRun
            Invoke-ExternalCommand -FilePath "docker" -Arguments @("push", $latestRef) -Description "Push da tag latest" -DryRun:$DryRun
        }

        Write-Host ""
        Write-Host "Processo concluido com sucesso." -ForegroundColor Green
        Write-Host "Imagem publicada: $imageRef"
    }
    finally {
        Pop-Location
    }
}
catch {
    Write-Error $_.Exception.Message
    exit 1
}

