# Nacitone 1.4.9

## Fitur baru
- Kartu rekap mingguan lebih hidup: latar dari sampul lagu teratas, kartu lagu #1 besar, baris kaca, dan artis teratas berinisial
- Dashboard Dengar Bersama lebih hidup: cincin cahaya di header, kode ruangan bisa diketuk untuk disalin, avatar muncul beranimasi, host punya cincin denyut
- Lirik mengambang: bisa disembunyikan atau diminimalkan jadi gelembung, kaca dengan blur latar, area sentuh lebih besar
- Footer beranda dan pemutar lebih pekat, tidak terlalu bening

## Perbaikan
- Status pembaruan dalam aplikasi tidak lagi macet, sesi pemasang dibatalkan saat gagal, dan ada tombol buka di browser bila gagal
- Overlay lirik hilang saat pemutaran berhenti

# Nacitone 1.4.8

## Fitur baru
- Pembaruan dalam aplikasi: tombol Perbarui mengunduh APK dengan progres, memeriksa paket dan tanda tangan, lalu memasangnya. Versi ini perlu dipasang manual satu kali; setelah itu pembaruan bisa dari dalam aplikasi
- Tombol di layar pemutar menjadi footer melengkung berbahan kaca bening, seragam dengan footer beranda

## Perbaikan
- Lagu yang dipencet tidak lagi tertimpa antrean lama saat aplikasi dibuka kembali
- Posisi dan volume sesi lalu dipulihkan setelah antrean selesai dimuat, bukan memakai timer tetap

# Nacitone 1.4.7

## Fitur baru
- Lirik mengambang: lirik tampil di atas aplikasi lain, bisa digeser bebas, posisinya diingat, dan ketukan membuka Nacitone (Pengaturan, Pemutar, Lainnya)
- Footer navigasi melengkung dengan kaca bening
- Kartu Story rekap mingguan memakai latar hitam pekat dengan kartu kaca

## Perbaikan
- Lagu yang dipencet tidak bereaksi setelah aplikasi dibersihkan dari recent apps
- Musik tetap berbunyi setelah jendela aplikasi dihapus walau opsi hentikan musik aktif
- Antrean lama yang lambat tidak lagi menimpa antrean lagu yang baru dipilih
- Sisa antrean tidak lagi hilang bila lagu pertama gagal diputar
- Pemutaran lanjut otomatis setelah galat audio, misalnya saat pindah ke Bluetooth
- "Putar berikutnya" tidak lagi menghapus antrean saat pemutar IDLE
- Fokus audio selalu dilepas, dan crossfade tidak lagi macet setelah aplikasi ditutup
- Muat lanjut radio YouTube tidak lagi menggandakan antrean, radio album tidak lagi crash
- Galat yang sebelumnya senyap kini dicatat di log
- String "Reconnect" Dengar Bersama kini ikut terjemahan

## Lainnya
- Pembaruan dependensi dan GitHub Actions
- Workflow tes unit di CI

# Nacitone 1.4.6

## Tampilan
- Tema hitam pekat dengan kaca bening, tanpa warna ungu (aplikasi, widget, dan kartu rekap mingguan)
- Cahaya latar dibuat netral dan sangat tipis
- Bilah atas dan bawah antrean memakai kaca gelap, tidak lagi putih

## Perbaikan
- Aplikasi tidak lagi crash saat memilih "Putar berikutnya" atau menambah ke antrean sebelum pemutar siap
- Aplikasi tidak lagi crash saat ruang Dengar Bersama tertutup di tengah pesan masuk
- Tombol pemutar tidak lagi menjatuhkan aplikasi bila terjadi galat
- Impor cadangan lama yang rusak tidak lagi membuat aplikasi crash
- Beberapa pengecekan null yang rawan di layar utama, lirik, pembaruan, dan menu YouTube dirapikan

# Nacitone 1.4.5

## Tampilan
- Header Beranda transparan penuh, juga saat digulir
- Kartu grid di Pustaka dan layar lain lebih bulat (24 dp), sampul daftar 12 dp
- Footer (bilah navigasi bawah) transparan
- Kartu rekap mingguan: judul besar, angka serif, tiap lagu kartu kaca, foto artis dengan cincin untuk peringkat 1
- Dashboard Dengar Bersama: ikon kaca, titik status berdenyut, kode ruangan per karakter, avatar dan kolom isian kaca

# Nacitone 1.4.4

## Tampilan
- Cahaya latar lembut (biru, ungu, merah muda) di belakang seluruh aplikasi agar efek kaca terlihat
- Header Beranda dan Pustaka transparan, dengan logo di sebelah kiri judul
- Ubin kosong di Pustaka dan mini player memakai kaca bening
- Lingkaran kosong di bilah atas dihapus

# Nacitone 1.4.3

## Gerak
- Sampul di pemutar bernapas pelan saat lagu diputar dan menyusut saat dijeda
- Baris pengaturan menyusut sedikit saat ditekan lalu memantul kembali

## Tampilan
- Sudut komponen diseragamkan ke tiga ukuran: 12, 20, dan 28 dp
- Status kosong berupa lingkaran kaca besar dengan ikon berwarna utama

# Nacitone 1.4.2

## Tampilan
- Bilah atas bergaya kaca di semua layar: latar transparan, judul tebal, tombol kembali dalam lingkaran kaca
- Bilah pencarian berbentuk pil kaca
- Semua field teks memakai kaca bening dengan sudut membulat

# Nacitone 1.4.1

## Logo
- Logo disederhanakan: hanya huruf n-not, transparan, tanpa ubin dan latar
- Latar ikon adaptif transparan; ikon toko, banner TV, dan berkas logo ikut diganti

# Nacitone 1.4.0

## Logo
- Logo baru: huruf n yang sekaligus not, di atas ubin kaca
- Diganti di ikon aplikasi, ikon notifikasi, banner TV, ikon toko, dan berkas logo di repo

## Tampilan
- Semua kartu, grup pengaturan, chip, dialog, dan navigasi memakai kaca putih bening, bukan ungu
- Grup pengaturan menjadi satu kartu kaca dengan pemisah tipis dan chip ikon
- Tombol tonal, chip terpilih, dan garis tepi mengikuti warna kaca dari tema

## Pembersihan
- Kode mati dan entri dependensi yang tidak terpakai dibuang

# Nacitone 1.3.0

## Pembaruan
- Pemberitahuan versi baru kini aktif secara bawaan: dialog saat aplikasi dibuka dan notifikasi

## Tampilan
- Mini player semi transparan dengan tint ungu, satu gaya saja
- Latar pemutar selalu gradien dari warna cover
- Sampul selalu dipangkas persegi
- Slider bergelombang dan efek cahaya lirik aktif secara bawaan
- Opsi gaya latar mini player, gaya latar pemutar, dan pangkas sampul dihapus

## Pembersihan
- Kode dan string dari opsi yang dihapus dibuang
