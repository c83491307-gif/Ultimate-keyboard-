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
  adb shell dumpsys window windows > "$RESULTS_DIR/windows.txt" 2>&1 || true
  adb shell dumpsys package "$PACKAGE" > "$RESULTS_DIR/package.txt" 2>&1 || true
}

fail() {
  local exit_code="${2:-1}"
  echo "::error::$1"
  capture_diagnostics
  exit "$exit_code"
}

test -s "$APK_PATH" || fail "Debug APK not found: $APK_PATH"
adb wait-for-device

# ADB connectivity can precede full Android boot. Allow the workflow's
# configured 20-minute emulator boot window and report the measured duration.
BOOT_TIMEOUT=1200
BOOT_START=$SECONDS
while :; do
  BOOT_STATE="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
  if [[ "$BOOT_STATE" == "1" ]]; then
    break
  fi
  BOOT_ELAPSED=$((SECONDS - BOOT_START))
  if (( BOOT_ELAPSED >= BOOT_TIMEOUT )); then
    echo "Emulator boot did not complete after ${BOOT_ELAPSED}s (limit: ${BOOT_TIMEOUT}s)."
    fail "Android emulator boot completion timed out" 2
  fi
  sleep 2
done
BOOT_ELAPSED=$((SECONDS - BOOT_START))
echo "Emulator boot completed in ${BOOT_ELAPSED}s."

# Confirm PackageManager is responsive before attempting APK installation.
PM_READY=false
for attempt in $(seq 1 12); do
  if adb shell pm list packages >/dev/null 2>&1; then
    PM_READY=true
    echo "Package Manager ready on attempt ${attempt}/12."
    break
  fi
  echo "Package Manager not ready; retry ${attempt}/12."
  sleep 5
done
if [[ "$PM_READY" != true ]]; then
  echo "Package Manager did not become responsive after 12 attempts."
  fail "Android Package Manager readiness timed out" 3
fi

adb logcat -c

echo "Installing NovaKeys debug APK..."
adb install -r "$APK_PATH"
adb shell pm path "$PACKAGE" | grep -q "package:" || fail "NovaKeys package is not installed"

echo "Launching the app and checking for startup crashes..."
adb shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1

APP_PID=""
for attempt in $(seq 1 60); do
  APP_PID="$(adb shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r' || true)"
  if [[ -n "$APP_PID" ]]; then break; fi
  sleep 1
done
[[ -n "$APP_PID" ]] || fail "NovaKeys process did not start within 60 seconds after launch"

echo "Enabling NovaKeys as the active emulator input method..."
adb shell ime enable "$IME_COMPONENT"
adb shell ime set "$IME_COMPONENT"
DEFAULT_IME="$(adb shell settings get secure default_input_method | tr -d '\r')"
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

# Android emulators can report the host Activity as resumed while another window
# still owns input focus (for example, the Settings screen used during IME setup).
# Derive a tap point from the live emulator resolution, near the top editor field,
# instead of relying on a device-specific hard-coded coordinate.
SCREEN_SIZE="$(adb shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -n 1)"
SCREEN_WIDTH="${SCREEN_SIZE%x*}"
SCREEN_HEIGHT="${SCREEN_SIZE#*x}"
if [[ "$SCREEN_WIDTH" =~ ^[0-9]+$ && "$SCREEN_HEIGHT" =~ ^[0-9]+$ ]]; then
  TAP_X=$((SCREEN_WIDTH / 2))
  TAP_Y=$((SCREEN_HEIGHT / 6))
  echo "Tapping editor using detected emulator size ${SCREEN_SIZE} at ${TAP_X},${TAP_Y}..."
  adb shell input tap "$TAP_X" "$TAP_Y"
else
  fail "Could not determine emulator screen size: $SCREEN_SIZE"
fi
sleep 2

# The debug host also requests the keyboard after its editor owns focus.
IME_VISIBLE=false
for attempt in $(seq 1 60); do
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
if grep -Eq 'FATAL EXCEPTION|Fatal signal [0-9]+|Process .*com\.creazzy\.novakeys\.debug.* has died' "$RESULTS_DIR/logcat.txt"; then
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
