Param(
    [string]$Profile = "prod",
    [string]$JarPath = "target/shiyu-backend.jar"
)

$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

Write-Host "[RC] 清理并打包..."
mvn -DskipTests clean package

if (!(Test-Path $JarPath)) {
    throw "未找到构建产物: $JarPath"
}

Write-Host "[RC] 终止旧进程..."
Get-CimInstance Win32_Process | Where-Object { $_.Name -eq "java.exe" -and $_.CommandLine -like "*${JarPath}*" } | ForEach-Object {
    Stop-Process -Id $_.ProcessId -Force
}

Write-Host "[RC] 启动新版本..."
Start-Process -FilePath "java" -ArgumentList "-jar $JarPath --spring.profiles.active=$Profile" -WindowStyle Hidden

Write-Host "[RC] 发布完成，建议检查: /api/v1/health/ping 与 /api/v1/health/readiness"
