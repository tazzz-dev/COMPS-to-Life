# COMPS to Life 🎓🎮

**COMPS to Life** adalah game RPG simulasi bertema kehidupan perkuliahan di Universitas Pembangunan Nasional Veteran Jakarta (UPNVJ), khususnya pada Fakultas Ilmu Komputer (FIK). Game ini dibuat menggunakan framework **libGDX** dan dirancang untuk mensimulasikan perjalanan seorang mahasiswa baru mulai dari pengumuman hasil SNBT hingga menjelajahi lingkungan kampus, berinteraksi dengan teman serta dosen, dan menyelesaikan kuis akademik.

---

## 🌟 Fitur Utama

- **📖 Story & Entrance Simulation**: Memulai game dengan simulasi pengumuman masuk perguruan tinggi (Hasil SNBT).
- **🗺️ Eksplorasi Kampus UPNVJ**: Jelajahi peta kampus yang detail, termasuk:
  - *Map UPNVJ (Luar Ruangan)*
  - *Selasar FIK (Fakultas Ilmu Komputer)*
  - *Denah Ruangan Kelas*
- **💬 Interaksi NPC & Kuis Akademik**: Berinteraksi dengan berbagai karakter NPC (Ayu, Nadhifa, Pak Hendra, Arya, dll.) untuk memperoleh petunjuk dan menyelesaikan kuis yang menantang guna meningkatkan skor akademik Anda.
- **🧥 Sistem Almamater (Almet)**: Buka dan kenakan Jaket Almamater (Almet) UPNVJ yang prestisius setelah mencapai skor kelulusan kuis akademik (> 75) dan menyelesaikan kuis dari Pak Hendra. Menggunakan almet akan merubah sprite dan animasi karakter Anda di dalam game.
- **🎒 Inventaris & Koleksi Item**: Temukan dan kumpulkan berbagai buku referensi, modul perkuliahan, brosur kompetisi, dan plakat yang tersebar di area kampus.
- **💾 Sistem Autopreserve (Database Lokal)**: Skor dan progres game (koordinat posisi terakhir, status almet, daftar kuis selesai) otomatis tersimpan menggunakan SQLite.
- **🎵 Audio & Atmosfer Imersif**: Dilengkapi musik latar in-game dan efek suara khusus yang dinamis (seperti auto-pause musik saat popup Almet didapatkan untuk transisi suara get-item yang jernih).

---

## 📂 Struktur Proyek

Proyek ini menggunakan arsitektur multi-modul standar **libGDX**:

*   **`core`**: Berisi seluruh logika utama game, layar (Screens), aset database, manajemen stage, serta algoritma pergerakan karakter dan NPC.
*   **`lwjgl3`**: Launcher utama untuk menjalankan game pada platform desktop (Windows, macOS, Linux) menggunakan LWJGL3.
*   **`assets`**: Berisi aset game seperti sprite lembar karakter, musik latar, efek suara, font, serta file peta `.tmx` (Tiled Map Editor).

---

## 🚀 Cara Menjalankan Game

Game ini menggunakan **Gradle** untuk manajemen dependensi dan build otomatis. Pastikan Anda telah menginstal **Java Development Kit (JDK 8 atau versi terbaru)** di perangkat Anda.

### 1. Menjalankan Game (Desktop)
Buka terminal/PowerShell di direktori root proyek ini, lalu jalankan perintah berikut:

**Windows (PowerShell):**
```powershell
.\gradlew lwjgl3:run
```

**Linux / macOS:**
```bash
./gradlew lwjgl3:run
```

### 2. Membuat File Executable (.JAR)
Untuk mengepack game menjadi file jar mandiri yang siap dijalankan:

**Windows:**
```powershell
.\gradlew lwjgl3:jar
```

**Linux / macOS:**
```bash
./gradlew lwjgl3:jar
```
File `.jar` hasil kompilasi akan berada di direktori `lwjgl3/build/libs/`.

---

## 🛠️ Alat Pengembangan (Development Tools)

*   **Framework**: [libGDX](https://libgdx.com/)
*   **Bahasa Pemrograman**: Java
*   **Build Tool**: Gradle
*   **Map Editor**: [Tiled Map Editor](https://www.mapeditor.org/) (untuk mengedit peta `.tmx`)
*   **Database**: SQLite

---

## 📝 Kontrol Permainan

*   **Arah/Pergerakan**: Tombol Panah (`Arrow Keys`) atau `W`, `A`, `S`, `D` untuk menggerakkan karakter.
*   **Interaksi**: Tekan `ENTER` atau `SPACE` untuk berinteraksi dengan NPC, membaca buku petunjuk, atau menutup jendela hint.
*   **Menu Jeda (Pause Menu)**: Tekan tombol jeda di layar UI.
*   **Kembali / Tutup Menu**: Tekan `ESCAPE` untuk menutup inventaris atau dialog aktif.
