
$files = Get-ChildItem -Recurse -File -Include *.java, *.properties, *.yml, *.yaml, *.xml, *.md, *.gradle
foreach ($f in $files) {
    $content = Get-Content $f.FullName -Raw
    $hasChange = $false
    if ($content -match "com\.zurich\.santander\.simulator\.prestador") {
        $content = $content -replace "com\.zurich\.santander\.simulator\.prestador", "com.zurich.prestador"
        $hasChange = $true
    }
    if ($content -match "com\.zurich\.santander\.simulator") {
        $content = $content -replace "com\.zurich\.santander\.simulator", "com.zurich.prestador"
        $hasChange = $true
    }
    if ($content -match "(?i)simulador") {
        $content = $content -replace "(?i)simulador", "prestador"
        $hasChange = $true
    }
    if ($content -match "(?i)simulator") {
        $content = $content -replace "(?i)simulator", "prestador"
        $hasChange = $true
    }
    if ($hasChange) {
        Set-Content -Path $f.FullName -Value $content -NoNewline
    }
}

