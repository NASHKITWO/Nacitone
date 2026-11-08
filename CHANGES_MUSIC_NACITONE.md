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

## Perbaikan crash (1.0.0)
- `MusicService.onStartCommand` mengakses `player` yang belum dibuat bila `onCreate` berhenti lebih awal (foreground ditolak, mis. vivo V2205/Android 14 saat widget ditekan). Ditambah penjagaan `::player.isInitialized`.

## Audit keamanan (tinjauan kode, 6 putaran)
- Lalu lintas teks biasa (cleartext) dimatikan; hanya localhost/127.0.0.1/10.0.2.2.
- `datastore/` (kunci API OpenRouter/DeepL, sesi Last.fm, kata sandi proxy) dikeluarkan dari backup cloud Android.
- Tautan dalam (deep link): ID playlist/album/channel divalidasi (`[A-Za-z0-9_-]{1,128}`) sebelum dipakai di rute navigasi.
- `ACTION_ALARM_TRIGGER` pada service yang diekspor hanya diproses bila ID alarm ada di penyimpanan alarm.
- Tautan Listen Together kini menampilkan dialog konfirmasi sebelum bergabung (kode divalidasi).
- Ekspor backup tidak lagi menyertakan kunci API (OpenRouter, DeepL), sesi Last.fm, kata sandi proxy, token Listen Together, dan cookie/visitor data. Setelah restore, kunci itu perlu diisi ulang.

## Dukungan pengembang
- Banner "Suka Nacitone?" di Beranda (komponen `SupportBanner`), ditutup = tidak tampil 30 hari; tombol Dukung membuka `https://sociabuzz.com/nacitwo/tribe`.
- Halaman Tentang: teks ajakan di atas tombol "Dukung Muhammad Ikhsan". URL didefinisikan satu kali (`SUPPORT_URL`).

## Audit keamanan putaran 2
- `provider_paths.xml`: hanya `cache` dan `Documents/NacitoneExports/`; akses seluruh penyimpanan eksternal dicabut.
- Updater hanya menerima berkas rilis dari `https://github.com/NASHKITWO/Nacitone/releases/download/`.
- Target widget (ACTION_OPEN_WIDGET_TARGET) divalidasi sebelum dipakai sebagai rute navigasi.

## Pembersihan sisa login, kebijakan privasi, Dependabot
- Dibuang: sakelar "sinkron ke YouTube" di CreatePlaylistDialog, parameter `allowSyncing`, cek cookie "sudah login" di AddToPlaylistDialog, LibraryPlaylistsScreen, StatsScreen, HomeScreen, helper `isSyncEnabled`/`isUserLoggedIn`; `SyncUtils` kini hanya `createPlaylist` (lokal) dan `clearPodcastData`.
- Sengaja dibiarkan: riwayat remote di HistoryScreen, HomeSection.AccountPlaylists, alur akun di HomeViewModel, penanganan cookie di App.kt, pratinjau akun di BackupAndRestore.
- Baru: `PRIVACY.md` (+ tautan "Kebijakan privasi" di Tentang), `.github/dependabot.yml` (gradle + github-actions); `renovate.json` dihapus agar tidak ada PR ganda.

## Audit keamanan putaran 3
- Restore backup kini menyaring pengaturan: proxy, URL server AI, URL server Listen Together, serta semua kunci/token/cookie dibuang dari berkas backup yang tidak tepercaya (mencegah backup berbahaya mengalihkan lalu lintas atau kunci API ke server penyerang).
- Berkas pengaturan/alias di backup dibatasi 2 MB (anti zip-bomb).
- Ekspor/impor pengaturan memakai API okio sesuai DataStore 1.2.
- Diperiksa, tidak ditemukan masalah: zip-slip (entri dicocokkan nama persis), WebView (jaringan diblokir), SQL (parameter Room), PendingIntent, log rahasia.
- Diketahui, belum diubah: tombol widget bisa dipicu aplikasi lain (hanya play/pause/suka); kunci API belum dienkripsi Keystore; tautan berbagi Listen Together masih memakai domain metrolist.cc.

## Penyegaran tampilan (desain mewah)
- `ui/theme/Type.kt` baru: tipografi (judul serif editorial, isi sans-serif dengan spasi huruf dan tinggi baris lebih lega) dan bentuk sudut yang lebih lembut (8/12/18/26/34 dp); dipasang di `NacitoneTheme`.
- Warna dasar bawaan diganti ke ungu kerajaan `#6A4CE0` (sesuai ikon); Android 12+ tetap mengikuti warna sistem/sampul lagu seperti sebelumnya.
- Kartu pengaturan: sudut 28 dp, garis tepi tipis, ikon dengan latar gradasi, judul grup berhuruf renggang.
- Banner dukungan: kartu gradasi dengan garis tepi emas.
- Font eksternal tidak ditambahkan (unduhan diblokir di lingkungan kerja); bila ingin, taruh .ttf di `res/font` dan ganti `Display`/`Body` di `Type.kt`.

## Perbaikan widget / tombol play
- Widget musik, playlist, dan turntable memakai `startForegroundService` (bukan `startService`) saat menekan tombol; lebih andal di vivo/Xiaomi/Oppo.
- Tombol play di widget saat antrean kosong kini memutar lagu favorit (atau terpopuler); bila pustaka kosong, membuka aplikasi.

## Penutupan celah yang tersisa
- Tombol widget (play/pause/suka/berikutnya/sebelumnya/putar target) kini lewat `WidgetActionReceiver` yang `exported=false`; aksi tersebut dihapus dari receiver widget yang diekspor, sehingga aplikasi lain tidak bisa memicunya.
- Tautan undangan Listen Together memakai `nacitone://listen?code=...` (bukan domain metrolist.cc); filter manifest diganti.

## Fitur baru (rekomendasi)
- Laporkan masalah: `utils/BugReport.kt`; crash terakhir disimpan di `filesDir/last_crash.txt`; tombol di Tentang membuka pemilih aplikasi dengan lampiran laporan (tidak ada pengiriman otomatis).
- Rak Nusantara di Beranda: `ui/component/NusantaraShelf.kt` (13 kartu gradasi: dangdut, koplo, campursari, gamelan, pop Sunda, pop Jawa, Minang, Batak, Bali, Banyuwangi, Melayu, Ambon & Papua, Qasidah).
- Kartu Rekap Mingguan didesain ulang bergaya "Wrapped" (`utils/WeeklyRecapImage.kt`).
- Penunjuk pemakaian data: `utils/DataUsageMeter.kt` (TransferListener pada sumber data jaringan), tampil di Pengaturan Pemutar.
- README ditulis ulang.
- Belum: desain ulang pemutar (butuh tangkapan layar).

## Satu ikon, satu aplikasi
- Build debug tidak lagi memakai akhiran ID `.debug` dan label "Nacitone Debug"; ikon dan nama sama dengan rilis. (Catatan: karena tanda tangan debug berbeda dari rilis, APK debug tidak bisa dipasang menimpa rilis; hapus salah satunya lebih dulu.)

## Pop-up dukungan
- `SupportPopup()` (ui/component/SupportBanner.kt): dialog dukungan untuk Muhammad Ikhsan di Beranda; paling cepat tiap 24 jam, sekali per pembukaan aplikasi, tertunda 2,5 detik; tombol "Dukung Muhammad Ikhsan" (Sociabuzz) dan "Nanti saja". Kunci: `SupportPopupShownAtKey`.

## Kenyamanan dan performa
- Banner dan pop-up dukungan menunggu DataStore selesai dibaca (`rememberLoadedLong`): tidak ada kedipan banner atau pop-up salah muncul di awal.
- Interval pop-up dukungan 24 jam.
- `Timber.DebugTree` hanya dipasang di build debug (rilis lebih ringan, logcat tidak membocorkan detail).
- `DataUsageMeter` menyimpan status jaringan 5 detik, bukan memanggil ConnectivityManager di setiap potongan data.
- Diperiksa: pemanggilan `runBlocking` tersisa semuanya berada di thread latar (query database/layanan), tidak di thread UI.

## Berbagi lagu
- `utils/SongShare.kt`: tombol Bagikan (menu lagu, antrean, pemutar, menu lagu online) kini mengirim judul, artis, cuplikan 4 baris lirik (bila sudah tersimpan di perangkat), tautan putar, dan jejak "Dibagikan lewat Nacitone", bukan hanya tautan YouTube Music. Kartu lirik gambar tetap lewat layar lirik.

## Situs web, impor Spotify
- Tautan "Situs web" (https://nacitone.xo.je) ditambahkan di layar Tentang, README, dan kebijakan privasi. Situs milik pengembang sendiri; tidak ada berkas situs di proyek ini.
- Tautan undangan Listen Together tetap `nacitone://listen?code=KODE` (tidak bergantung pada halaman web).
- Impor Spotify: item "Impor dari Spotify" (panduan Exportify + pilih CSV) dan deteksi otomatis kolom (Track Name / Artist Name(s)) pada impor CSV.
- Mini-player/pemutar: belum didesain ulang (menunggu tangkapan layar).

## Hasil 20 penguji (putaran 2)
- Widget tetap bisa memutar setelah aplikasi di-swipe dari recents (MusicService.onStartCommand).
- Impor CSV kosong tidak lagi membuat dialog progres menggantung.
- Tautan Listen Together hanya diterima untuk `nacitone://listen`.
- Backup: kode akun/cookie dihapus (previewBackup, fetchAccountInfoFromBackup, clearAuthData, UI akun). Restore tidak lagi memanggil jaringan.
- Aksesibilitas: contentDescription untuk tombol berikutnya/sebelumnya/bagikan/opsi/suka/timer tidur di pemutar & mini-player.
- Home: layar error + tombol "Coba lagi" bila konten gagal dimuat.
- Keamanan: perintah kontrol widget ke MusicService butuh token acak per proses (aplikasi lain ditolak).
- Listen Together: keluar dari ruangan lama sebelum bergabung lewat tautan; kode diubah ke huruf besar.
- release.yml: gagal bila versi input ≠ versionName. Terjemahan ID untuk 5 string.
- Listen Together: hanya server wss:// (selain itu kembali ke bawaan).
- Restore: batas ukuran DB 2 GB, batal bila layanan musik gagal berhenti, null-check intent restart.
- BugReport: tidak crash bila tidak ada aplikasi pengirim.
- Widget: tombol play saat antrean kosong menunggu 1,5 dtk pemulihan antrean agar tidak menimpa antrean pengguna.
- Pop-up dukungan tidak muncul saat pemakaian pertama (hitung 24 jam mulai dari sana).
- Crash buka-ulang & widget: scope MusicService punya CoroutineExceptionHandler (error pemulihan antrean/widget dicatat, tidak crash); jalur "stale restart" memanggil startForeground sebelum stopSelf; loop progres widget tidak lagi mati/crash saat widget/launcher bermasalah.
- Desain/aksesibilitas: rak Nusantara (peran tombol + label klik, tinggi mengikuti ukuran font, gradasi Koplo/Gamelan lebih gelap untuk kontras); ResizableIconButton punya parameter contentDescription; tombol sebelumnya/berikutnya/suka di pemutar penuh diberi label.
- Rak Nusantara dihapus (kode, string, README). File tak terpakai dihapus: extensions/ContextExt.kt, crowdin.yml, assets/badges, assets/blacksmith-powered.png.
- Pop-up dukungan: status tampil disimpan global (tidak hilang saat Beranda dibuat ulang); waktu dicatat saat ditutup; muncul juga di pembukaan pertama.
- Antislop: banner dukungan jadi kartu tonal datar (tanpa border emas/gradasi), teks lebih manusiawi; ikon pengaturan memakai warna tonal datar, bukan gradasi.
- Teks tumpang tindih: item daftar & mini-player tinggi minimum (bukan tetap); tombol timer tidur memakai Button (bukan IconButton 40dp); slot kanan item Pengaturan dibatasi lebar; jarak huruf label dikurangi; tombol "Putar semua", kartu Daily Discover, Wrapped Top 5, header Tentang diberi batas lebar/maxLines; teks judul widget playlist memakai dp.
- Pembersihan: 10 string tak terpakai (di semua bahasa), 16 query DAO tak terpakai, konstanta dimensi/lirik/volume/error tak terpakai, fungsi BugReport & widget yang tak dipanggil dihapus.
