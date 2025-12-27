# Test script: Test login and get user info flow

Write-Host "=== Testing Login and User Info Flow ==="

# 1. Get captcha
Write-Host "1. Getting captcha..."
try {
    $codeResponse = Invoke-WebRequest -Uri "http://localhost:8011/v1/web/user/code" -Method GET -UseBasicParsing
    $codeData = $codeResponse.Content | ConvertFrom-Json
    $uuid = $codeData.uuid
    Write-Host "   UUID: $uuid"
} catch {
    Write-Host "   Failed to get captcha: $($_.Exception.Message)"
    exit 1
}

# 2. Login (using test account, captcha is temporarily empty)
Write-Host "2. Logging in..."
$loginBody = @{
    username = "admin"
    password = "123456"
    uuid = $uuid
    code = "1234"  # Captcha temporarily uses a fixed value
}

try {
    $loginResponse = Invoke-WebRequest -Uri "http://localhost:8011/v1/web/user/login" -Method POST -Body ($loginBody | ConvertTo-Json) -ContentType "application/json" -UseBasicParsing
    $loginData = $loginResponse.Content | ConvertFrom-Json
    $token = $loginData.token
    Write-Host "   Token: $token"
} catch {
    Write-Host "   Login failed: $($_.Exception.Message)"
    if ($_.Response) {
        Write-Host "   Response content: $($_.Response.Content)"
    }
    exit 1
}

# 3. Call /info API with token
Write-Host "3. Calling /info API..."
try {
    $infoResponse = Invoke-WebRequest -Uri "http://localhost:8011/v1/web/user/info" -Method GET -Headers @{Authorization = "Bearer $token"} -UseBasicParsing
    Write-Host "   Status code: $($infoResponse.StatusCode)"
    Write-Host "   Response content: $($infoResponse.Content)"
    Write-Host "   Test passed!"
} catch {
    Write-Host "   Failed to call /info API: $($_.Exception.Message)"
    if ($_.Response) {
        Write-Host "   Response content: $($_.Response.Content)"
    }
    exit 1
}

Write-Host "=== Test Completed ==="
