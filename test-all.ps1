param(
    [switch]$Install
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$FrontendRoot = Join-Path $ProjectRoot "frontend"
$BackendRoot = Join-Path $ProjectRoot "backend"

if (-not (Test-Path (Join-Path $FrontendRoot "package.json"))) {
    throw "Could not find frontend\package.json. Keep test-all.ps1 in the Library Lane project root beside the frontend and backend folders."
}

if (-not (Test-Path (Join-Path $BackendRoot "mvnw.cmd"))) {
    throw "Could not find backend\mvnw.cmd. Keep test-all.ps1 in the Library Lane project root beside the frontend and backend folders."
}

if ($Install) {
    Push-Location $FrontendRoot
    try {
        npm install
        if ($LASTEXITCODE -ne 0) {
            throw "npm install failed with exit code $LASTEXITCODE."
        }
        npx playwright install chromium
        if ($LASTEXITCODE -ne 0) {
            throw "Playwright browser installation failed with exit code $LASTEXITCODE."
        }
    }
    finally {
        Pop-Location
    }
}

Push-Location $BackendRoot
try {
    .\mvnw.cmd test
    if ($LASTEXITCODE -ne 0) {
        throw "Backend tests failed with exit code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}

Push-Location $FrontendRoot
try {
    npm run test:all
    if ($LASTEXITCODE -ne 0) {
        throw "Frontend tests failed with exit code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}

Write-Host ""
Write-Host "Library Lane deterministic automation passed." -ForegroundColor Green
