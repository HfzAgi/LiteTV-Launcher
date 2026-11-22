#!/bin/sh
# Uji koneksi ADB-ke-diri-sendiri di emulator: adbd dibuka di TCP 5555, lalu BersihSTB diminta
# menyambung ke 127.0.0.1:5555 dan menjalankan "id". Hasil ditulis sebagai ADB-SELFTEST-RESULT.
adb tcpip 5555 || true
sleep 8
adb wait-for-device
adb logcat -c
adb shell am force-stop com.litetv.bersih
adb shell am start -n com.litetv.bersih/.MainActivity --es selftest 1
sleep 20
adb logcat -d | grep -E "BERSIH|AndroidRuntime|FATAL" | tail -25
if adb logcat -d | grep -q "BERSIH.*SELFTEST OK"; then
  echo "ADB-SELFTEST-RESULT: PASS"
else
  echo "ADB-SELFTEST-RESULT: FAIL"
fi
exit 0
