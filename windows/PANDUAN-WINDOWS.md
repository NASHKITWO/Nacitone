# Menjalankan Nacitone di Windows

Nacitone adalah aplikasi **Android** (Kotlin, Jetpack Compose, Media3, Room). Aplikasi ini tidak bisa jadi
file `.exe` biasa tanpa ditulis ulang. Cara yang realistis di Windows: build APK, lalu jalankan di emulator Android.

## Cara tercepat

1. Pasang **Android Studio** (`winget install -e --id Google.AndroidStudio`), buka sekali agar SDK terunduh.
2. Buat emulator: *Device Manager > Create Device*, pilih Pixel, image **x86_64 API 34 atau lebih baru**, lalu Start.
   (Perlu virtualisasi aktif: aktifkan "Windows Hypervisor Platform" dan virtualisasi di BIOS.)
3. Salin folder `windows` ini ke root project, lalu di PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File .\windows\Setup-Windows.ps1
```

Skrip akan: cek JDK 21, cek Android SDK, build `:app:assembleFossDebug`, menyalin hasilnya ke `dist\Nacitone.apk`,
memasangnya via adb, dan menjalankan aplikasinya.

## Alternatif emulator

| Pilihan | Cara |
|---|---|
| BlueStacks / LDPlayer / MuMu | Seret `dist\Nacitone.apk` ke jendelanya, atau jalankan skrip dengan `-Target 127.0.0.1:5555` (aktifkan ADB di pengaturan emulator) |
| Hanya build APK | `.\windows\Setup-Windows.ps1 -BuildOnly` |
| Windows Subsystem for Android | Sudah dihentikan Microsoft (Maret 2025), jangan diandalkan |

## Versi Windows native (.exe)

Butuh port ke Compose Multiplatform Desktop: ganti Media3/ExoPlayer, Room, service/notifikasi Android, dan
modul `innertube` (saat ini `com.android.library`). Ini proyek besar tersendiri, bukan sekadar mengubah konfigurasi build.
