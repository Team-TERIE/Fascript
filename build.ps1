$jarName = "TERIE-Fascript-1.0.0.jar"
$jarPath = "build\libs\$jarName"
$outputPath = "bin"

gradle clean build --stacktrace

if ($LASTEXITCODE) { Write-Host "Build Failed"; exit 1 }

if (-not (Test-Path $jarPath)) {
    Write-Host "Build Failed: artifact not found at $jarPath"
    exit 1
}

New-Item -ItemType Directory -Path $outputPath -Force | Out-Null
Copy-Item $jarPath "$outputPath\$jarName" -Force

Write-Host "Build Success"
