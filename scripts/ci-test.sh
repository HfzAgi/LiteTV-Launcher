#!/bin/sh
# Dijalankan di emulator: pasang APK, buka, pastikan tidak crash.
# Pemakaian: ci-test.sh <apk> <package> <home|launcher> [screenshot.png] [activity]
set -e
APK="$1"; PKG="$2"; MODE="$3"; SHOT="${4:-screen.png}"; ACT="${5:-.MainActivity}"
adb install -r "$APK"
adb shell pm list packages | grep -q "$PKG"
adb logcat -c
if [ "$MODE" = "home" ]; then
  adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.HOME -n "$PKG/$ACT"
else
  adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n "$PKG/$ACT"
fi
sleep 8
if adb logcat -d | grep -q "FATAL EXCEPTION"; then
  echo "GAGAL: crash terdeteksi"; adb logcat -d | grep -A30 "FATAL EXCEPTION"; exit 1
fi
PID=$(adb shell pidof "$PKG" | tr -d '\r')
[ -n "$PID" ] || { echo "GAGAL: proses $PKG tidak berjalan"; adb logcat -d | grep -E "$PKG|AndroidRuntime|Process.*Sending signal|am_kill|am_crash|am_proc_died" | head -40; exit 1; }
adb exec-out screencap -p > "$SHOT"
echo "OK: $PKG terpasang dan berjalan (pid $PID)"
