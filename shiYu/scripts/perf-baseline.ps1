Param(
    [string]$BaseUrl = "http://127.0.0.1:8083",
    [string]$Token = "",
    [string]$SignatureSecret = "shiyu-signature-key-2026",
    [int]$Users = 20,
    [int]$DurationSeconds = 30,
    [switch]$IncludeCreateOrder,
    [string]$CreateOrderBodyPath = "",
    [switch]$IncludePayOrder,
    [long]$PayOrderId = 0,
    [int]$PayChannel = 1
)

$ErrorActionPreference = "Stop"

function Invoke-Scenario {
    Param(
        [string]$Name,
        [string]$Method,
        [string]$Path,
        [int]$Concurrency,
        [int]$Seconds,
        [bool]$NeedAuth,
        [bool]$NeedSignature,
        [string]$Body
    )

    Write-Host "[Perf] 场景: $Name, 并发=$Concurrency, 时长=${Seconds}s"

    $endAt = (Get-Date).AddSeconds($Seconds)
    $jobs = @()
    for ($i = 0; $i -lt $Concurrency; $i++) {
        $jobs += Start-Job -ScriptBlock {
            Param($BaseUrlInner, $MethodInner, $PathInner, $BodyInner, $NeedAuthInner, $NeedSignatureInner, $TokenInner, $SecretInner, $EndAt)
            $ok = 0
            $fail = 0
            while ((Get-Date) -lt $EndAt) {
                try {
                    $headers = @{ "Content-Type" = "application/json" }
                    if ($NeedAuthInner) {
                        $headers["Authorization"] = "Bearer $TokenInner"
                    }

                    if ($NeedSignatureInner) {
                        $ts = [int][double]::Parse((Get-Date -UFormat %s))
                        $nonce = [Guid]::NewGuid().ToString("N")
                        $payload = "$MethodInner`n$PathInner`n$ts`n$nonce`n$SecretInner"
                        $sha = [System.Security.Cryptography.SHA256]::Create()
                        $bytes = [System.Text.Encoding]::UTF8.GetBytes($payload)
                        $hash = $sha.ComputeHash($bytes)
                        $signature = -join ($hash | ForEach-Object { $_.ToString("x2") })

                        $headers["X-Timestamp"] = "$ts"
                        $headers["X-Nonce"] = $nonce
                        $headers["X-Signature"] = $signature
                        $headers["X-Idempotency-Key"] = [Guid]::NewGuid().ToString("N")
                    }

                    $uri = "$BaseUrlInner$PathInner"
                    if ([string]::IsNullOrWhiteSpace($BodyInner)) {
                        $resp = Invoke-RestMethod -Method $MethodInner -Uri $uri -Headers $headers
                    } else {
                        $resp = Invoke-RestMethod -Method $MethodInner -Uri $uri -Headers $headers -Body $BodyInner
                    }

                    if ($null -ne $resp -and $resp.code -eq 0) {
                        $ok++
                    } else {
                        $fail++
                    }
                } catch {
                    $fail++
                }
            }
            return @{ ok = $ok; fail = $fail }
        } -ArgumentList $BaseUrl, $Method, $Path, $Body, $NeedAuth, $NeedSignature, $Token, $SignatureSecret, $endAt
    }

    $totalOk = 0
    $totalFail = 0
    Receive-Job -Job $jobs -Wait -AutoRemoveJob | ForEach-Object {
        $totalOk += [int]$_.ok
        $totalFail += [int]$_.fail
    }

    $total = $totalOk + $totalFail
    $failRate = if ($total -eq 0) { 0 } else { [Math]::Round(($totalFail * 100.0) / $total, 2) }
    Write-Host "[Perf][$Name] total=$total ok=$totalOk fail=$totalFail failRate=${failRate}%"
}

Invoke-Scenario -Name "商品列表" -Method "GET" -Path "/api/v1/products?page=1&page_size=20" -Concurrency $Users -Seconds $DurationSeconds -NeedAuth $false -NeedSignature $false -Body ""

if ($IncludeCreateOrder.IsPresent) {
    if ([string]::IsNullOrWhiteSpace($Token)) {
        throw "启用 IncludeCreateOrder 时必须传入 -Token"
    }
    if ([string]::IsNullOrWhiteSpace($CreateOrderBodyPath) -or !(Test-Path $CreateOrderBodyPath)) {
        throw "启用 IncludeCreateOrder 时必须提供有效的 -CreateOrderBodyPath"
    }
    $createBody = Get-Content -Raw -Path $CreateOrderBodyPath
    Invoke-Scenario -Name "下单接口" -Method "POST" -Path "/api/v1/orders" -Concurrency ([Math]::Max(1, [int]($Users / 2))) -Seconds $DurationSeconds -NeedAuth $true -NeedSignature $true -Body $createBody
}

if ($IncludePayOrder.IsPresent) {
    if ([string]::IsNullOrWhiteSpace($Token)) {
        throw "启用 IncludePayOrder 时必须传入 -Token"
    }
    if ($PayOrderId -le 0) {
        throw "启用 IncludePayOrder 时必须传入有效的 -PayOrderId"
    }

    $payBody = "{\"pay_channel\":$PayChannel}"
    Invoke-Scenario -Name "支付发起接口" -Method "POST" -Path "/api/v1/orders/$PayOrderId/pay" -Concurrency ([Math]::Max(1, [int]($Users / 2))) -Seconds $DurationSeconds -NeedAuth $true -NeedSignature $true -Body $payBody
}

Write-Host "[Perf] 基线压测结束"
