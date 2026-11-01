<#
 Setup-Windows.ps1
 Build Nacitone lalu pasang ke Android emulator (atau perangkat) dari Windows.

 Cara pakai (dari root project):
   powershell -ExecutionPolicy Bypass -File .\windows\Setup-Windows.ps1
   powershell -ExecutionPolicy Bypass -File .\windows\Setup-Windows.ps1 -Target 127.0.0.1:5555
   powershell -ExecutionPolicy Bypass -File .\windows\Setup-Windows.ps1 -BuildOnly
#>
param(
  [string]$Target = "",     # alamat adb emulator pihak ketiga, mis. 127.0.0.1:5555
  [switch]$BuildOnly        # hanya build APK, tanpa memasang
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Step($m) { Write-Host ""; Write-Host "==> $m" -ForegroundColor Cyan }
function Has($c)  { [bool](Get-Command $c -ErrorAction SilentlyContinue) }

# 1. JDK 21
Step "Cek JDK 21"
$javaOk = $false
if (Has java) {
  $first = (cmd /c "java -version 2>&1" | Select-Object -First 1)
  if ($first -match '"(\d+)') { $javaOk = ([int]$Matches[1] -ge 21) }
}
if (-not $javaOk) {
  if (Has winget) {
    Write-Host "JDK 21 belum ada. Memasang Temurin 21 via winget..."
    winget install -e --id EclipseAdoptium.Temurin.21.JDK --accept-package-agreements --accept-source-agreements
    Write-Host "Selesai. TUTUP lalu buka ulang terminal, kemudian jalankan skrip ini lagi." -ForegroundColor Yellow
  } else {
    Write-Host "Pasang JDK 21 (Temurin) dari https://adoptium.net lalu jalankan ulang skrip." -ForegroundColor Yellow
  }
  exit 1
}
Write-Host "JDK OK"

# 2. Android SDK
Step "Cek Android SDK"
$sdk = $env:ANDROID_HOME
if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
if (-not $sdk) { $sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk" }
if (-not (Test-Path $sdk)) {
  Write-Host "Android SDK tidak ditemukan di: $sdk" -ForegroundColor Yellow
  Write-Host "Pasang Android Studio (winget install -e --id Google.AndroidStudio), buka sekali sampai SDK terunduh, lalu jalankan ulang."
  exit 1
}
Write-Host "SDK: $sdk"
$sdkFwd = $sdk -replace '\\', '/'
Set-Content -Path (Join-Path $root "local.properties") -Value "sdk.dir=$sdkFwd" -Encoding ASCII

$sdkmanager = Join-Path $sdk "cmdline-tools\latest\bin\sdkmanager.bat"
if (Test-Path $sdkmanager) {
  Step "Menerima lisensi SDK"
  try { (1..60 | ForEach-Object { "y" }) | & $sdkmanager --licenses | Out-Null } catch { Write-Host "Lisensi dilewati: $_" }
}

# 3. Build
Step "Build APK (pertama kali bisa 10-20 menit)"
& (Join-Path $root "gradlew.bat") ":app:assembleFossDebug"
if ($LASTEXITCODE -ne 0) { Write-Host "Build gagal." -ForegroundColor Red; exit $LASTEXITCODE }

$apk = Get-ChildItem -Path (Join-Path $root "app\build\outputs\apk") -Recurse -Filter "*.apk" |
       Where-Object { $_.Name -like "*universal*foss*debug*" } |
       Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $apk) {
  $apk = Get-ChildItem -Path (Join-Path $root "app\build\outputs\apk") -Recurse -Filter "*.apk" |
         Sort-Object LastWriteTime -Descending | Select-Object -First 1
}
if (-not $apk) { Write-Host "APK tidak ditemukan." -ForegroundColor Red; exit 1 }

$dist = Join-Path $root "dist"
New-Item -ItemType Directory -Force -Path $dist | Out-Null
Copy-Item $apk.FullName (Join-Path $dist "Nacitone.apk") -Force
Write-Host "APK siap: $dist\Nacitone.apk" -ForegroundColor Green

if ($BuildOnly) { exit 0 }

# 4. Pasang via adb
Step "Memasang ke emulator/perangkat"
$adb = Join-Path $sdk "platform-tools\adb.exe"
if (-not (Test-Path $adb)) {
  if (Has adb) { $adb = (Get-Command adb).Source }
  else { Write-Host "adb tidak ditemukan. Seret Nacitone.apk ke jendela emulator secara manual." -ForegroundColor Yellow; exit 0 }
}
if ($Target) { & $adb connect $Target }
& $adb devices
& $adb install -r (Join-Path $dist "Nacitone.apk")
if ($LASTEXITCODE -ne 0) {
  Write-Host "Gagal memasang. Pastikan emulator sudah berjalan (lihat PANDUAN-WINDOWS.md)." -ForegroundColor Red
  exit $LASTEXITCODE
}
& $adb shell monkey -p com.nacitone.music.debug -c android.intent.category.LAUNCHER 1 | Out-Null
Write-Host "Terpasang dan dijalankan." -ForegroundColor Green
