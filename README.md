# LiteTV Launcher 1.0 — ZTE B866F V1.1 / Android TV 14

Proyek **native Kotlin** sederhana tanpa dependensi runtime pihak ketiga. Tampilan: wallpaper pegunungan statis, jam, tanggal, daftar aplikasi Android TV, fokus remote D-pad dan tombol OK. Tidak mengandung iklan, analitik, koneksi jaringan, atau service latar.

## Build APK di Windows

1. Install Android Studio versi modern dengan JDK 17 dan Android SDK Platform 35.
2. Buka folder `LiteTV` dengan **Open** (bukan hanya file Kotlin).
3. Tunggu Gradle Sync. Gradle 8.9+ dan Android Gradle Plugin 8.7.3/Kotlin 2.0.21 dapat diunduh oleh Android Studio. Proyek sengaja tidak menyertakan Gradle wrapper binary; Android Studio dapat mengonfigurasi Gradle bila diperlukan.
4. Pilih **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. APK debug berada di `app/build/outputs/apk/debug/app-debug.apk` setelah build berhasil.
6. Dengan ADB terhubung: `adb install -r app-debug.apk` (gunakan path APK yang benar).
7. Buka LiteTV dari daftar aplikasi TV. Uji arah/OK/Back, aplikasi streaming, Wi-Fi, dan restart.
8. Untuk menguji Home: **Settings > Apps > Default apps > Home app**, jika menu tersedia. Pilih LiteTV. Bila tidak ada pilihan atau firmware mengunci Home, **jangan disable launcher bawaan**; LiteTV tetap bisa dibuka dari daftar aplikasi.

## ADB dan debloat (opsional)

1. Aktifkan Developer Options dan debugging jika firmware mengizinkan. Hubungkan dengan kabel atau jaringan ADB yang sudah diotorisasi.
2. Salin `adb.exe`, `AdbWinApi.dll`, dan `AdbWinUsbApi.dll` dari Platform Tools ke folder `scripts`, atau tambahkan Platform Tools ke PATH.
3. Jalankan PowerShell dari folder `scripts`:

```powershell
.\LiteTV-ADB.ps1 -Mode Inventory
.\LiteTV-ADB.ps1 -Mode Status
.\LiteTV-ADB.ps1 -Mode Disable -Package nama.paket.yang.sudah.diverifikasi
.\LiteTV-ADB.ps1 -Mode Rollback
```

**Tidak ada daftar paket otomatis**: inventaris harus diperiksa dulu, karena paket IndiHome/ZTE berbeda antar-firmware. Script memblokir sebagian paket kritis, tetapi daftar blokir tidak menjamin paket lain aman. Jangan menonaktifkan launcher bawaan, System UI, Google Play Services, layanan input remote, jaringan, DRM, OTA, atau paket ZTE yang belum diketahui. Uji satu paket setiap kali.

Backup ini **hanya inventaris paket dan log perubahan**, bukan cadangan data/firmware. Rollback memerlukan ADB tetap dapat terhubung dan paket masih terpasang.

## Catatan kompatibilitas

- Identifikasi model berdasarkan foto: **B866F V1.1, Android TV 14**, bukan V2F Android 11.
- `targetSdk 28` dipakai untuk kompatibilitas instalasi Android 14; Android 14 menolak APK yang menargetkan SDK di bawah 23. Untuk distribusi Play Store, target SDK perlu dinaikkan sesuai kebijakan terbaru.
- Android TV firmware tertentu mungkin membatasi pemilihan default Home atau izin ADB.
- Ini adalah source code yang belum diuji langsung pada STB. Build APK harus dilakukan di Android Studio dan diuji pada perangkat.
- Wallpaper `mountains.png` adalah ilustrasi orisinal yang dibuat secara programatis, bukan foto pihak ketiga.
- Untuk kembali ke launcher semula, pilih Home app sebelumnya dari Settings jika tersedia, atau uninstall LiteTV setelah memastikan launcher lama tetap aktif.
