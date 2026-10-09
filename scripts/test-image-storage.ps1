#Requires -Version 7.0
param([string]$R2EnvFile)
$ErrorActionPreference = 'Stop'
$repository = Split-Path -Parent $PSScriptRoot
$container = 'jandi-storage-smoke-' + [Guid]::NewGuid().ToString('N').Substring(0, 8)
$created = $false
$api = [System.Net.Http.HttpClient]::new()
$public = [System.Net.Http.HttpClient]::new()
$api.Timeout = [TimeSpan]::FromSeconds(15)
$public.Timeout = [TimeSpan]::FromSeconds(15)
$baseUrl = 'http://localhost:19091/test-bucket'
$legacyBase = 'https://old-images.example.com'
$dockerOptions = @()
$command = @()
if ($R2EnvFile) {
    $R2EnvFile = (Resolve-Path -LiteralPath $R2EnvFile).Path
    $settings = @{}
    foreach ($line in [IO.File]::ReadAllLines($R2EnvFile)) {
        if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
            $key, $value = $line.Split('=', 2)
            $settings[$key] = $value
        }
    }
    if (-not $settings.IMAGE_STORAGE_BUCKET.EndsWith('-dev')) { throw 'R2 smoke requires a development bucket' }
    $baseUrl = $settings.IMAGE_STORAGE_PUBLIC_URL.TrimEnd('/')
    $legacyBase = $settings.IMAGE_STORAGE_LEGACY_PUBLIC_URLS.Split(',')[0].TrimEnd('/')
    $settings.Clear()
    $dockerOptions = @('--env-file', $R2EnvFile)
    $command = @('sh', '-c', "redis-server --bind 127.0.0.1 --daemonize yes && exec java -cp 'build/storage-smoke:build/storage-smoke/*' com.jandi.band_backend.image.LocalStorageSmokeServer --external-r2")
}
try {
    docker build --target storage-smoke -t jandi-band:storage-smoke $repository
    if ($LASTEXITCODE -ne 0) { throw 'Storage smoke image build failed' }
    docker create --name $container -p 127.0.0.1:18081:8080 -p 127.0.0.1:19091:9091 @dockerOptions jandi-band:storage-smoke @command | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Storage smoke container creation failed' }
    $created = $true
    docker start $container | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Storage smoke container start failed' }
    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        docker exec $container test -s /tmp/storage-smoke-token 2>$null
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
        Start-Sleep -Milliseconds 500
    }
    if (-not $ready) {
        docker logs --tail 40 $container
        throw 'Storage smoke fixture did not become ready'
    }
    $token = (docker exec $container cat /tmp/storage-smoke-token).Trim()
    $api.DefaultRequestHeaders.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $token)
    $payload = [Convert]::FromBase64String('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=')
    $multipart = [System.Net.Http.MultipartFormDataContent]::new()
    $file = [System.Net.Http.ByteArrayContent]::new($payload)
    $file.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::new('image/png')
    $multipart.Add($file, 'file', 'smoke.png')
    $multipart.Add([System.Net.Http.StringContent]::new('smoke/한글 + 100%'), 'dirName')
    $upload = $api.PostAsync('http://localhost:18081/api/images/upload', $multipart).GetAwaiter().GetResult()
    if (-not $upload.IsSuccessStatusCode) {
        throw ('Upload HTTP ' + [int]$upload.StatusCode + ': ' + $upload.Content.ReadAsStringAsync().GetAwaiter().GetResult())
    }
    $result = $upload.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
    if (-not $result.success) { throw 'Image API reported an upload failure' }
    $url = [string]$result.data
    if (-not $url.StartsWith($baseUrl + '/smoke/')) { throw 'Unexpected public image URL' }
    if ($url.Contains('+') -or -not $url.Contains('%2B')) { throw 'Literal plus must be encoded for S3 public URLs' }
    $download = $public.GetAsync($url).GetAwaiter().GetResult()
    $download.EnsureSuccessStatusCode() | Out-Null
    $downloaded = $download.Content.ReadAsByteArrayAsync().GetAwaiter().GetResult()
    if ([Convert]::ToBase64String($downloaded) -ne [Convert]::ToBase64String($payload)) { throw 'Image bytes differ' }
    if ($download.Content.Headers.ContentType.MediaType -ne 'image/png') { throw 'Content-Type was not preserved' }
    $key = $url.Substring($baseUrl.Length + 1)
    $default = $baseUrl + '/club-photo/rhythmeet.webp'
    foreach ($ignored in @(('https://foreign.example.com/' + $key), $default, ($legacyBase + '/club-photo/rhythmeet.webp'))) {
        $deleted = $api.DeleteAsync('http://localhost:18081/api/images?fileUrl=' + [Uri]::EscapeDataString($ignored)).GetAwaiter().GetResult()
        $deleted.EnsureSuccessStatusCode() | Out-Null
    }
    foreach ($preserved in @($url, $default)) {
        $check = $public.GetAsync($preserved).GetAwaiter().GetResult()
        $check.EnsureSuccessStatusCode() | Out-Null
    }
    $legacy = $legacyBase + '/' + $key
    $deleted = $api.DeleteAsync('http://localhost:18081/api/images?fileUrl=' + [Uri]::EscapeDataString($legacy)).GetAwaiter().GetResult()
    $deleted.EnsureSuccessStatusCode() | Out-Null
    $missing = $public.GetAsync($url).GetAwaiter().GetResult()
    if ([int]$missing.StatusCode -ne 404) { throw 'Object still exists after deletion' }
    Write-Output 'PASS: authenticated API upload, Unicode key, exact bytes, Content-Type, external/default protection, legacy URL delete, final 404'
} catch {
    if ($created) { docker logs --tail 50 $container }
    throw
} finally {
    $api.Dispose()
    $public.Dispose()
    if ($created) { docker rm -f $container | Out-Null }
}
