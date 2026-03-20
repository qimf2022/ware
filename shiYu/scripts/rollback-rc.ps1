Param(
    [Parameter(Mandatory = $true)]
    [string]$BackupJar,
    [string]$Profile = "prod",
    [string]$CurrentJar = "target/shiyu-backend.jar"
)

$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

if (!(Test-Path $BackupJar)) {
    throw "回滚包不存在: $BackupJar"
}

Write-Host "[Rollback] 终止当前进程..."
Get-CimInstance Win32_Process | Where-Object { $_.Name -eq "java.exe" -and $_.CommandLine -like "*${CurrentJar}*" } | ForEach-Object {
    Stop-Process -Id $_.ProcessId -Force
}

Write-Host "[Rollback] 恢复备份包..."
Copy-Item -Path $BackupJar -Destination $CurrentJar -Force

Write-Host "[Rollback] 重启服务..."
Start-Process -FilePath "java" -ArgumentList "-jar $CurrentJar --spring.profiles.active=$Profile" -WindowStyle Hidden

Write-Host "[Rollback] 完成，建议检查: /api/v1/health/ping 与 /api/v1/health/readiness"
