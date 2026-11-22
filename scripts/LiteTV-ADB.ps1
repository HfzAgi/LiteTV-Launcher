param([ValidateSet('Inventory','Disable','Rollback','Status')][string]$Mode='Inventory',[string]$Package='')
$ErrorActionPreference='Stop'
$adb=Join-Path $PSScriptRoot 'adb.exe'
if (!(Test-Path $adb)) { $adb='adb' }
$dir=Join-Path $PSScriptRoot 'backup'
New-Item -ItemType Directory -Force -Path $dir | Out-Null
function RunAdb([string[]]$argv) { $output = & $adb @argv 2>&1; if ($LASTEXITCODE -ne 0) { throw "ADB gagal: $output" }; return @($output) }
$devices=RunAdb @('devices')
$ready=@($devices | Where-Object { $_ -match '\sdevice$' })
if ($ready.Count -ne 1) { throw 'Hubungkan tepat satu STB berstatus device. Jalankan adb devices untuk memeriksa.' }
$serial=($ready[0] -split '\s+')[0]
function Shell([string[]]$args) { RunAdb (@('-s',$serial,'shell')+$args) }
$all=@(Shell @('pm','list','packages') | Where-Object { $_ -match '^package:' })
$installed=@($all | ForEach-Object { $_.Substring(8) })
$log=Join-Path $dir 'disabled-by-litetv.txt'
switch($Mode) {
 'Inventory' {
  $all | Set-Content (Join-Path $dir 'packages-all.txt')
  Shell @('pm','list','packages','-s') | Set-Content (Join-Path $dir 'packages-system.txt')
  Shell @('pm','list','packages','-3') | Set-Content (Join-Path $dir 'packages-user.txt')
  Shell @('pm','list','packages','-d') | Set-Content (Join-Path $dir 'packages-disabled-before.txt')
  Shell @('getprop','ro.build.fingerprint') | Set-Content (Join-Path $dir 'build-fingerprint.txt')
  Write-Host "Inventaris tersimpan di $dir. Kirim packages-all.txt untuk penilaian paket."
 }
 'Status' { Shell @('pm','list','packages','-d') }
 'Disable' {
  if ($Package -notmatch '^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z0-9_]+)+$') { throw 'Masukkan -Package nama.paket.valid' }
  if ($installed -notcontains $Package) { throw 'Paket tidak terpasang.' }
  $protected=@('com.google.android.gms','com.android.systemui','com.android.settings','com.android.vending','com.google.android.tvlauncher','com.google.android.apps.tv.launcherx','com.litetv.launcher')
  if ($protected -contains $Package -or $Package -match '^(com\.zte\.|com\.android\.(inputmethod|providers|permissioncontroller)|com\.google\.android\.gms)') { throw 'Paket kritis/dibatasi. Tidak diizinkan oleh script.' }
  $disabled=@(Shell @('pm','list','packages','-d') | ForEach-Object { $_ -replace '^package:','' })
  if ($disabled -contains $Package) { throw 'Paket sudah nonaktif sebelum tindakan ini; tidak dicatat untuk rollback.' }
  $answer=Read-Host "Nonaktifkan $Package untuk user 0? ketik YA"
  if ($answer -cne 'YA') { Write-Host 'Dibatalkan'; break }
  $result=Shell @('pm','disable-user','--user','0',$Package)
  if (($result -join ' ') -notmatch 'disabled-user') { throw "Tidak ada konfirmasi disabled-user: $result" }
  Add-Content -Path $log -Value $Package
  Write-Host "Dinonaktifkan: $Package. Uji remote, Home, Wi-Fi, audio dan aplikasi setelah restart."
 }
 'Rollback' {
  if (!(Test-Path $log)) { Write-Host 'Tidak ada perubahan yang dicatat.'; break }
  $entries=@(Get-Content $log | Where-Object { $_ -match '^\S+$' } | Select-Object -Unique)
  foreach ($pkg in $entries) {
   if ($installed -contains $pkg) {
    try { Shell @('pm','enable','--user','0',$pkg) | Out-Null; Write-Host "Dipulihkan: $pkg" }
    catch { Write-Warning "Gagal memulihkan $pkg : $_" }
   }
  }
  Write-Host 'Periksa status. File log tidak dihapus agar ada jejak audit.'
 }
}
