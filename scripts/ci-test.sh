#!/bin/sh
# Dijalankan di emulator: pasang APK, buka sebagai Home, pastikan tidak crash.
set -e
APK="$1"
adb install -r "$APK"
adb shell pm list packages | grep -q com.litetv.launcher
adb logcat -c
adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.HOME -n com.litetv.launcher/.MainActivity
sleep 6
PID=$(adb shell pidof com.litetv.launcher | tr -d '\r')
[ -n "$PID" ] || { echo "GAGAL: proses launcher tidak berjalan"; adb logcat -d | tail -60; exit 1; }
if adb logcat -d | grep -q "FATAL EXCEPTION"; then
  echo "GAGAL: crash terdeteksi"; adb logcat -d | grep -A25 "FATAL EXCEPTION"; exit 1
fi
adb shell dumpsys activity activities | grep -i "litetv" | head -5
adb exec-out screencap -p > screen.png
echo "OK: terpasang dan berjalan (pid $PID)"
