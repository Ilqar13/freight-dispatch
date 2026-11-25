<#
.SYNOPSIS
    Windows-da PostGIS DLL xətasını düzəldir.

.DESCRIPTION
    Xəta: ERROR: could not load library "...\lib\postgis-3.dll": The specified module could not be found.

    Səbəb: postgis-3.dll -> libgeos_c.dll -> libwinpthread-1.dll zənciri.
    PostGIS paketi libwinpthread-1.dll-i bin\postgisgui\-yə qoyur, bin\-ə qoymur,
    buna görə postgres.exe DLL-i tapa bilmir ("module could not be found").

    Skript DLL-i bin\-ə kopyalayır və PostgreSQL servisini yenidən başladır.
    Administrator hüququ tələb olunur - skript özü UAC pəncərəsini açır.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\fix-postgis-windows.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\fix-postgis-windows.ps1 -PgRoot 'C:\Program Files\PostgreSQL\16'
#>
param(
    [string]$PgRoot = 'C:\Program Files\PostgreSQL\17',
    [switch]$NoRestart
)

$ErrorActionPreference = 'Stop'

function Test-Admin {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal $identity
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

if (-not (Test-Admin)) {
    Write-Host 'Administrator hüququ lazımdır - açılan UAC pəncərəsini təsdiqləyin...'
    $params = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', "`"$PSCommandPath`"",
                '-PgRoot', "`"$PgRoot`"")
    if ($NoRestart) { $params += '-NoRestart' }
    Start-Process -FilePath 'powershell.exe' -Verb RunAs -ArgumentList $params
    return
}

$bin        = Join-Path $PgRoot 'bin'
$postgisDll = Join-Path $PgRoot 'lib\postgis-3.dll'
$target     = Join-Path $bin 'libwinpthread-1.dll'

if (-not (Test-Path -LiteralPath $postgisDll)) {
    throw "postgis-3.dll tapılmadı ($postgisDll). Əvvəlcə PostGIS paketini quraşdırın (Stack Builder)."
}

if (Test-Path -LiteralPath $target) {
    Write-Host "Artıq mövcuddur: $target"
} else {
    $candidates = @(
        (Join-Path $bin 'postgisgui\libwinpthread-1.dll'),
        (Join-Path $PgRoot 'pgAdmin 4\runtime\libwinpthread-1.dll')
    )
    $source = $candidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
    if (-not $source) {
        throw 'libwinpthread-1.dll tapılmadı. PostGIS paketini Stack Builder ilə yenidən quraşdırın.'
    }
    Copy-Item -LiteralPath $source -Destination $target -Force
    Write-Host "Kopyalandı: $source"
    Write-Host "         -> $target"
}

if (-not $NoRestart) {
    $service = Get-CimInstance Win32_Service |
        Where-Object {
            $_.PathName -like "*$PgRoot*" -and
            ($_.PathName -like '*pg_ctl.exe*' -or $_.PathName -like '*postgres.exe*')
        } |
        Select-Object -First 1
    if ($service) {
        Restart-Service -Name $service.Name -Force
        Write-Host "Servis yenidən başladıldı: $($service.Name)"
    } else {
        Write-Host 'PostgreSQL servisi tapılmadı - servisi əl ilə yenidən başladın.'
    }
}

Write-Host ''
Write-Host 'Yoxlama:'
Write-Host '  psql -U postgres -d freight -c "CREATE EXTENSION IF NOT EXISTS postgis;"'
Write-Host '  psql -U postgres -d freight -c "SELECT postgis_full_version();"'
