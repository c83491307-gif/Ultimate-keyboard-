#!/usr/bin/env bash
set -euo pipefail

APK_PATH="${1:-app-debug.apk}"
PACKAGE="com.creazzy.novakeys.debug"
IME_COMPONENT="${PACKAGE}/com.creazzy.novakeys.ime.NovaInputMethodService"
HOST_COMPONENT="${PACKAGE}/com.creazzy.novakeys.testing.ImeSmokeTestActivity"
RESULTS_DIR="emulator-test-results"
mkdir -p "$RESULTS_DIR"

capture_diagnostics() {
  adb logcat -d -v threadtime > "$RESULTS_DIR/logcat.txt" 2>&1 || true
  adb shell dumpsys input_method > "$RESULTS_DIR/input-method.txt" 2>&1 || true
  adb shell dumpsys activity activities > "$RESULTS_DIR/activities.txt" 2>&1 || true
}

fail() {
  echo "::error::$1"
  capture_diagnostics
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

APP_PID=""
for attempt in $(seq 1 60); do
  APP_PID="$(adb shell pidof "$PACKAGE" 2>/dev/null | tr -d '\\r' || true)"
  if [[ -n "$APP_PID" ]]; then break; fi
  sleep 1
done
[[ -n "$APP_PID" ]] || fail "NovaKeys process did not start within 60 seconds after launch"

echo "Enabling NovaKeys as the active emulator input method..."
adb shell ime enable "$IME_COMPONENT"
adb shell ime set "$IME_COMPONENT"
DEFAULT_IME="$(adb shell settings get secure default_input_method | tr -d '\\r')"
[[ "$DEFAULT_IME" == "$IME_COMPONENT" ]] || fail "Expected default IME $IME_COMPONENT, got: $DEFAULT_IME"

echo "Opening a real focused text editor and explicitly requesting the keyboard..."
adb shell am start -n "$HOST_COMPONENT" >/dev/null
# Wait until ActivityManager reports the host activity before testing IME state.
HOST_VISIBLE=false
for attempt in $(seq 1 45); do
  if adb shell dumpsys activity activities 2>/dev/null | grep -Fq "$HOST_COMPONENT"; then
    HOST_VISIBLE=true
    break
  fi
  sleep 1
done
[[ "$HOST_VISIBLE" == true ]] || fail "IME smoke host activity did not become visible"

# The test activity calls showSoftInput after its window receives focus. A tap
# also exercises the normal user path and avoids a false failure if focus is delayed.
adb shell input tap 420 190 || true
IME_VISIBLE=false
for attempt in $(seq 1 30); do
  adb shell dumpsys input_method > "$RESULTS_DIR/input-method.txt" 2>&1 || true
  if grep -Fq "mCurId=$IME_COMPONENT" "$RESULTS_DIR/input-method.txt" &&
     grep -Eiq 'mInputShown=true|mShowRequested=true|inputShown=true' "$RESULTS_DIR/input-method.txt"; then
    IME_VISIBLE=true
    break
  fi
  sleep 1
done

if [[ "$IME_VISIBLE" != true ]]; then
  fail "NovaKeys did not bind as the current IME and become visible in the focused editor"
fi

echo "Checking app process and Android runtime crash log..."
adb shell pidof "$PACKAGE" >/dev/null || fail "NovaKeys process died during IME activation"
adb logcat -d -v threadtime > "$RESULTS_DIR/logcat.txt"
if grep -Eq 'FATAL EXCEPTION|Fatal signal [0-9]+|Process .*com\\.creazzy\\.novakeys\\.debug.* has died' "$RESULTS_DIR/logcat.txt"; then
  fail "Android runtime crash detected; see uploaded emulator-test-results artifact"
fi

{
  echo "NovaKeys Android emulator smoke test: PASS"
  echo "Package: $PACKAGE"
  echo "Default IME: $DEFAULT_IME"
  echo "Bound IME: $IME_COMPONENT"
  echo "Host activity: $HOST_COMPONENT"
  adb shell getprop ro.build.version.release | sed 's/^/Android: /'
  adb shell getprop ro.build.version.sdk | sed 's/^/API: /'
} | tee "$RESULTS_DIR/summary.txt"

echo "Emulator smoke test passed."
