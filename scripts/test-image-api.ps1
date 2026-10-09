param(
    [Parameter(Mandatory = $true)][string]$R2EnvFile,
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$repoPath = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$configPath = (Resolve-Path -LiteralPath $R2EnvFile).Path
if ($configPath.StartsWith($repoPath + [System.IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw '개발 R2 환경 파일은 Git 저장소 밖에서 관리해야 합니다.'
}
$previousConfig = $env:IMAGE_API_TEST_ENV_FILE
$env:IMAGE_API_TEST_ENV_FILE = $configPath
$compose = @('compose', '-f', (Join-Path $repoPath 'compose.image-api-test.yaml'))
$started = $false
$testExit = 1
Push-Location $repoPath
try {
    $existing = & docker @compose ps -aq
    if ($LASTEXITCODE -ne 0) { throw '테스트 스택 상태 확인 실패' }
    if ($existing) { throw '동일한 테스트 스택이 실행 중입니다. 기존 검사 결과와 객체 정리를 먼저 확인하세요.' }
    if (-not $SkipBuild) {
        & docker @compose --profile test build
        if ($LASTEXITCODE -ne 0) { throw '테스트 이미지 빌드 실패' }
    }
    $started = $true
    & docker @compose up -d app
    if ($LASTEXITCODE -ne 0) { throw '테스트 앱 시작 실패' }
    & docker @compose --profile test run --rm runner
    $testExit = $LASTEXITCODE
    Write-Host '비밀값을 제외한 결과: build/image-api-tests/result-*.json'
} finally {
    if ($started) {
        & docker @compose down
        if ($LASTEXITCODE -ne 0) { $testExit = 1 }
    }
    $env:IMAGE_API_TEST_ENV_FILE = $previousConfig
    Pop-Location
}
exit $testExit
