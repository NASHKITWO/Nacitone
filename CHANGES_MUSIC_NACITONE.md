# Perubahan rebranding penuh Nacitone

- Package Kotlin: com.metrolist.* -> com.nacitone.* (music, innertube, lastfm, kugou, lrclib, paxsenix, shazamkit)
- applicationId: com.nacitone.music; folder schema Room dan proto ikut diganti
- Identifier: MetrolistTheme -> NacitoneTheme, Theme.Metrolist -> Theme.Nacitone, style widget, env METROLIST_* -> NACITONE_*
- File: metrolist_strings.xml -> nacitone_strings.xml (semua bahasa)
- Semua teks UI: Nacitone; halaman About hanya menampilkan Muhammad Ikhsan
- Update checker dimatikan default

Sengaja TIDAK diubah (agar fitur tetap jalan / lisensi aman):
- com.metrolist.innertubex dan com.github.MetrolistGroup (library eksternal dari JitPack)
- metrolist.cc (server Listen Together), metrolistdiscord:// (redirect OAuth Discord)
- User-Agent Paxsenix/LastFM, nama aset updater
- LICENSE GPL-3.0 dan header copyright di file sumber

Fitur dihapus:
- Discord Rich Presence (paket discord, pengaturan, OAuth, string, kunci preferensi)
- Login Google/YouTube Music, ganti channel, dan login token manual (LoginScreen, AccountScreen, AccountSettingsViewModel)
- Menu akun di Home kini hanya berisi Integrasi, Pengaturan, dan info pembaruan

Fitur ditambah:
- Bagian Kontak di halaman About (Instagram, email, GitHub)
- Kualitas audio terpisah untuk data seluler (hemat data)
- Baris Musik Indonesia di Home

Performa:
- Daftar Home memakai contentType agar komposisi item bisa dipakai ulang saat scroll
- Cache memori gambar Coil 15% -> 25%
- Build release lokal: ./gradlew :app:assembleFossRelease -PlocalRelease

Fitur baru (tahap 2):
- Kartu lirik: pilihan format Persegi atau Story 9:16 (zona aman Instagram), tulisan "dari Nacitone" di pojok
- Tema nusantara: Batik Sogan, Batik Nila, Kopi Tubruk, Wayang
- Rangkuman mingguan: layar baru, kartu di Home setiap Senin, menu akun, bagikan sebagai gambar 9:16
- Pemutar mengutamakan lirik: opsi di Pengaturan Pemutar, sampul mengecil di pojok kanan atas, kembali ke sampul penuh bila lirik tidak ada
- Bagikan playlist lewat QR: item baru di menu playlist (yang punya tautan), pratinjau QR dan kartu gambar 9:16 (dependensi baru: com.google.zxing:core)

## Pembersihan kode tidak terpakai (tahap akhir)
- Dihapus: `AccountViewModel` (yatim setelah login dihapus), fungsi lirik Punjabi (`isPunjabi`, `romanizePunjabi`, `GURMUKHI_ROMAJI_MAP`), `isWordSynced`, `isLineSynced`, `LyricsProviderRegistry.getProviderName`.
- Sebelumnya: `InfoLabel`, fungsi hapus-pustaka & `performFullSync` di `SyncUtils`, 3 metode DAO, 73 string dan 2 drawable.

## Tahap berikutnya
- Updater kini memeriksa rilis `NASHKITWO/Nacitone`; pemeriksaan Metrolist-KMP dihapus. Tes `UpdaterTest` ditulis ulang.
- `SyncUtils` dipangkas dari ±1.700 baris menjadi kerangka kecil (buat playlist lokal, bersihkan podcast, sisanya no-op). `PlaylistSyncTest` dihapus karena menguji logika sinkronisasi yang sudah tidak ada.
- Beranda: bagian berat (Daily Discover, Explore, komunitas, rekomendasi mirip) dimuat berurutan, bukan serentak.
- Ikon peluncur baru (not musik + gelombang suara, latar ungu); ikon legacy PNG/WebP dihapus (minSdk 26).
- User-agent, URL referer, judul fastlane, dan `changelog.md` diganti ke Nacitone. Header lisensi "Metrolist Project" dan dependensi `innertubex` sengaja dipertahankan (kewajiban GPL / pustaka).

## Rilis & tanda tangan
- Versi diatur ke 1.0.0 (versionCode 1).
- Penandatanganan rilis dibaca dari `keystore.properties` atau env (`STORE_FILE`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`); keystore di-ignore Git.
- Workflow baru `.github/workflows/release.yml`: tag `v*` -> build FOSS release -> rilis GitHub berisi `app-universal-release.apk` (nama yang dikenali pemeriksa update).
- `PANDUAN_RILIS.md` berisi langkah lengkap.
