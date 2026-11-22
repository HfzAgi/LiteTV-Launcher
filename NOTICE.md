# Pemberitahuan pihak ketiga

Folder `home/` dan sebagian `debloat/src/main/assets/profiles/` berasal dari proyek
**cleanyourtv** oleh Gathin (https://github.com/Gathin23/cleanyourtv), lisensi MIT.
- `home/`: launcher Home (dari `launcher/app`), tanpa unit test.
- `debloat/src/main/assets/profiles/*.txt` dan `index.tsv`: daftar debloat per model TV (dari `examples/*/debloat.txt` dan `tv.json`).
- Teks lisensi: `licenses/cleanyourtv-MIT.txt`, font Outfit/Lexend `licenses/FONT-LICENSE-OFL.txt`, ikon `licenses/ICONS-LICENSE-FLUENT-EMOJI.txt`.

Profil `zte-b866f.txt` dan modul `app/` (LiteTV) serta `debloat/` (BersihSTB) ditulis sendiri untuk repo ini.

## libadb-android
`debloat/src/main/java/com/litetv/bersih/AdbManager.java` disesuaikan dari aplikasi contoh
**libadb-android** (Muntashir Al-Islam, https://github.com/MuntashirAkon/libadb-android), lisensi ganda
GPL-3.0-or-later atau Apache-2.0; di sini dipakai di bawah **Apache-2.0** (`licenses/libadb-android-Apache-2.0.txt`).
Library itu, `sun-security-android`, dan Conscrypt dipasang sebagai dependensi Gradle.
