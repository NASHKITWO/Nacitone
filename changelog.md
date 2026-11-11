# Nacitone 1.0.8

## Tampilan
- Kartu di Dengar Bersama dan Rekap Mingguan kini transparan dengan satu warna tema, plus animasi masuk yang halus

## Perbaikan
- Perbaikan crash saat membuka aplikasi lagi setelah lama ditutup: antrean tersimpan yang rusak atau usang tidak lagi membuat pemutar error
- Layar Beranda tidak lagi crash saat lagu dihapus dari pustaka atau rekomendasi tidak punya album
- Daftar Riwayat, Artis, Pencarian, Podcast, dan Charts tidak lagi crash karena item ganda
- Artis dan album pada playlist online tidak lagi tertukar
- Alarm, widget pengenal musik, dan berbagi teks ke aplikasi tidak lagi bisa membuat aplikasi crash
- Slider tidak lagi error pada ukuran nol; tautan yang gagal dibuka tidak lagi crash

## Keamanan
- URL lirik dari server wajib HTTPS, parser XML menolak DOCTYPE, muatan Dengar Bersama dibatasi 4 MB, dan nama file ekspor playlist dibersihkan
- Widget pengenal musik tidak lagi bisa dipicu dari aplikasi lain

## Pustaka
- Pembaruan ktor, datastore, palette, kotlin, ksp, cast framework, dan gradle
