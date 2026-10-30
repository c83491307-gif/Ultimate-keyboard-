#!/usr/bin/env bash
set -euo pipefail

APK_PATH="${1:-app-debug.apk}"
PACKAGE="com.creazzy.novakeys.debug"
IME_COMPONENT="${PACKAGE}/com.creazzy.novakeys.ime.NovaInputMethodService"
HOST_COMPONENT="${PACKAGE}/com.creazzy.novakeys.testing.ImeSmokeTestActivity"
RESULTS_DIR="emulator-test-results"
mkdir -p "$RESULTS_DIR"

fail() {
  echo "::error::$1"
  adb logcat -d -v threadtime > "$RESULTS_DIR/logcat.txt" || true
  adb shell dumpsys input_method > "$RESULTS_DIR/input-method.txt" 2>&1 || true
  exit 1
}

test -s "$APK_PATH" || fail "Debug APK not found: $APK_PATH"
adb wait-for-device
adb shell getprop sys.boot_completed | grep -q "1" || fail "Android emulator did not finish booting"
adb logcat -c

echo "Installing NovaKeys debug APK..."
adb install -r "$APK_PATH"
adb shell pm path "$PACKAGE" | grep -q "package:" || fail "NovaKeys package is not installed"

echo "Launching the app and checking for startup crashes..."
adb shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1

# The emulator can be heavily loaded immediately after first boot. monkey may
# return before ActivityManager has finished creating the app process, so poll
# for startup instead of treating a short scheduling delay as an app crash.
APP_PID=""
for attempt in $(seq 1 60); do
  APP_PID="$(adb shell pidof "$PACKAGE" 2>/dev/null | tr -d '\\r' || true)"
  if [[ -n "$APP_PID" ]]; then
    break
  fi
  sleep 1
done
if [[ -z "$APP_PID" ]]; then
  fail "NovaKeys process did not start within 60 seconds after launch"
fi

echo "Enabling NovaKeys as the active emulator input method..."
adb shell ime enable "$IME_COMPONENT"
adb shell ime set "$IME_COMPONENT"
DEFAULT_IME="$(adb shell settings get secure default_input_method | tr -d '\r')"
[[ "$DEFAULT_IME" == "$IME_COMPONENT" ]] || fail "Expected default IME $IME_COMPONENT, got: $DEFAULT_IME"

echo "Opening a focused text editor so Android must bind and show the IME..."
adb shell am start -W -n "$HOST_COMPONENT"
sleep 3
adb shell dumpsys input_method > "$RESULTS_DIR/input-method.txt" 2>&1
grep -Fq "$IME_COMPONENT" "$RESULTS_DIR/input-method.txt" || fail "InputMethodManager is not reporting the NovaKeys service"
if ! grep -Eiq 'mInputShown=true|mShowRequested=true|inputShown=true' "$RESULTS_DIR/input-method.txt"; then
  fail "NovaKeys was selected, but the emulator did not report the keyboard as shown"
fi

echo "Checking app process and Android runtime crash log..."
adb shell pidof "$PACKAGE" >/dev/null || fail "NovaKeys process died during IME activation"
adb logcat -d -v threadtime > "$RESULTS_DIR/logcat.txt"
if grep -Eq 'FATAL EXCEPTION|Fatal signal [0-9]+|Process .*com\.creazzy\.novakeys\.debug.* has died' "$RESULTS_DIR/logcat.txt"; then
  fail "Android runtime crash detected; see uploaded emulator-test-results artifact"
fi

{
  echo "NovaKeys Android emulator smoke test: PASS"
  echo "Package: $PACKAGE"
  echo "Default IME: $DEFAULT_IME"
  echo "Host activity: $HOST_COMPONENT"
  adb shell getprop ro.build.version.release | sed 's/^/Android: /'
  adb shell getprop ro.build.version.sdk | sed 's/^/API: /'
} | tee "$RESULTS_DIR/summary.txt"

echo "Emulator smoke test passed."
