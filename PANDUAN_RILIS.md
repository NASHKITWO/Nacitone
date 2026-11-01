# Panduan rilis Nacitone

Dua hal yang harus ada agar pemeriksa update bekerja: **kunci penandatanganan** dan **rilis di GitHub**.

## 1. Buat keystore (sekali saja)

```
keytool -genkeypair -v -keystore nacitone-release.jks -alias nacitone ^
  -keyalg RSA -keysize 2048 -validity 10000
```

(Di Linux/macOS ganti `^` dengan `\`.) Isi kata sandi dan data diri saat diminta.

> **Simpan `nacitone-release.jks` dan kata sandinya di tempat aman (mis. password manager + salinan di flash disk).**
> Kalau hilang, pengguna yang sudah memasang aplikasi **tidak bisa update**: mereka harus uninstall dulu, dan datanya hilang.
> Jangan pernah commit berkas ini ke Git (sudah dimasukkan ke `.gitignore`).

## 2. Build rilis di komputer sendiri (opsional)

Buat berkas `keystore.properties` di folder utama proyek (sudah di-ignore Git):

```
storeFile=C:/path/ke/nacitone-release.jks
storePassword=KATA_SANDI_KEYSTORE
keyAlias=nacitone
keyPassword=KATA_SANDI_KUNCI
```

Lalu: `./gradlew :app:assembleFossRelease`. Hasil: `app/build/outputs/apk/foss/release/`.
Tanpa `keystore.properties`, build rilis tidak ditandatangani. Untuk uji performa saja gunakan `-PlocalRelease` (kunci debug).

## 3. Rilis otomatis lewat GitHub Actions

Di GitHub: repo -> Settings -> Secrets and variables -> Actions -> New repository secret. Isi:

| Secret | Isi |
|---|---|
| `KEYSTORE_BASE64` | hasil encode keystore (lihat di bawah) |
| `STORE_PASSWORD` | kata sandi keystore |
| `KEY_ALIAS` | `nacitone` |
| `KEY_PASSWORD` | kata sandi kunci |
| `LASTFM_API_KEY`, `LASTFM_SECRET` | opsional |

Encode keystore:
- Windows (PowerShell): `[Convert]::ToBase64String([IO.File]::ReadAllBytes("nacitone-release.jks")) | Set-Clipboard`
- Linux/macOS: `base64 -w0 nacitone-release.jks`

## 4. Menerbitkan versi baru

1. Naikkan `versionCode` (+1) dan `versionName` di `app/build.gradle.kts` (mis. `1.0.1`).
2. Tulis catatan di `changelog.md` (menjadi isi rilis di GitHub).
3. Commit dan push, lalu:
   ```
   git tag v1.0.1
   git push origin v1.0.1
   ```
   Atau jalankan manual: tab Actions -> "Release APK" -> Run workflow.
4. Workflow membuat rilis `v1.0.1` berisi `app-universal-release.apk`.

Pemeriksa update di aplikasi membaca rilis terbaru repo `NASHKITWO/Nacitone`, membandingkan nomor versinya dengan `versionName`, dan mengunduh aset bernama `app-universal-release.apk`. Karena itu nama berkas dan nomor versi tidak boleh diubah-ubah.

## Catatan
- Build FOSS (`assembleFossRelease`) memuat pemeriksa update. Varian `izzy` tidak.
- Karena turunan Metrolist (GPL-3.0), kode sumber harus tetap terbuka dan `LICENSE` ikut disertakan.
