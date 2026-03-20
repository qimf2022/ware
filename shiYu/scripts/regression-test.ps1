Param(
    [string]$BaseUrl = "http://127.0.0.1:8083",
    [string]$Token = "",
    [switch]$SkipAuthApis
)

$ErrorActionPreference = "Stop"

function Invoke-Api {
    Param(
        [string]$Name,
        [string]$Method,
        [string]$Path,
        [string]$Body = "",
        [bool]$NeedAuth = $false
    )

    $headers = @{ "Content-Type" = "application/json" }
    if ($NeedAuth -and !$SkipAuthApis.IsPresent) {
        if ([string]::IsNullOrWhiteSpace($Token)) {
            throw "接口 ${Name} 需要 Token，请通过 -Token 传入"
        }
        $headers["Authorization"] = "Bearer $Token"
    }

    $url = "$BaseUrl$Path"
    if ([string]::IsNullOrWhiteSpace($Body)) {
        $resp = Invoke-RestMethod -Method $Method -Uri $url -Headers $headers
    } else {
        $resp = Invoke-RestMethod -Method $Method -Uri $url -Headers $headers -Body $Body
    }

    if ($null -eq $resp -or $resp.code -ne 0) {
        $code = if ($null -eq $resp) { "null" } else { $resp.code }
        throw "[FAIL] ${Name} code=${code}"
    }

    Write-Host "[PASS] $Name"
}

Write-Host "[Regression] 开始接口回归..."

Invoke-Api -Name "健康检查" -Method "GET" -Path "/api/v1/health/ping"
Invoke-Api -Name "就绪检查" -Method "GET" -Path "/api/v1/health/readiness"
Invoke-Api -Name "首页配置" -Method "GET" -Path "/api/v1/home/config"
Invoke-Api -Name "商品列表" -Method "GET" -Path "/api/v1/products?page=1&page_size=5"
Invoke-Api -Name "搜索热词" -Method "GET" -Path "/api/v1/search/hot"
Invoke-Api -Name "可领优惠券" -Method "GET" -Path "/api/v1/coupons/available?page=1&page_size=5"
Invoke-Api -Name "专题详情" -Method "GET" -Path "/api/v1/topics/1"
Invoke-Api -Name "全局配置" -Method "GET" -Path "/api/v1/config"

if (!$SkipAuthApis.IsPresent) {
    Invoke-Api -Name "用户资料" -Method "GET" -Path "/api/v1/user/profile" -NeedAuth $true
    Invoke-Api -Name "订单列表" -Method "GET" -Path "/api/v1/orders?page=1&page_size=5" -NeedAuth $true
}

Write-Host "[Regression] 回归通过"
