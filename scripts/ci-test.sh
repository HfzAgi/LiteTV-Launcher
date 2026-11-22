#!/bin/sh
# Dijalankan di emulator: pasang APK, buka, pastikan tidak crash.
# Pemakaian: ci-test.sh <apk> <package> <home|launcher> [screenshot.png]
set -e
APK="$1"; PKG="$2"; MODE="$3"; SHOT="${4:-screen.png}"
adb install -r "$APK"
adb shell pm list packages | grep -q "$PKG"
adb logcat -c
if [ "$MODE" = "home" ]; then
  adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.HOME -n "$PKG/.MainActivity"
else
  adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n "$PKG/.MainActivity"
fi
sleep 8
PID=$(adb shell pidof "$PKG" | tr -d '\r')
[ -n "$PID" ] || { echo "GAGAL: proses $PKG tidak berjalan"; adb logcat -d | tail -60; exit 1; }
if adb logcat -d | grep -q "FATAL EXCEPTION"; then
  echo "GAGAL: crash terdeteksi"; adb logcat -d | grep -A25 "FATAL EXCEPTION"; exit 1
fi
adb exec-out screencap -p > "$SHOT"
echo "OK: $PKG terpasang dan berjalan (pid $PID)"
