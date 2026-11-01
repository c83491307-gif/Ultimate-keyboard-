#!/usr/bin/env bash
# NovaKeys Termux audit: build + unit tests + lint + optional live-device IME smoke test.
set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
STAMP="$(date +%Y%m%d-%H%M%S)"
REPORT_DIR="$ROOT/reports/termux-$STAMP"
mkdir -p "$REPORT_DIR"
REPORT="$REPORT_DIR/report.md"
FAILURES=0
WARNINGS=0
STARTED_AT="$(date -Iseconds)"
DEVICE_MODE=false
for arg in "$@"; do [[ "$arg" == "--device" ]] && DEVICE_MODE=true; done

section() { printf '\n## %s\n\n' "$1" | tee -a "$REPORT"; }
pass() { printf -- '- [PASS] %s\n' "$1" | tee -a "$REPORT"; }
warn() { WARNINGS=$((WARNINGS+1)); printf -- '- [WARN] %s\n' "$1" | tee -a "$REPORT"; }
fail() { FAILURES=$((FAILURES+1)); printf -- '- [FAIL] %s\n' "$1" | tee -a "$REPORT"; }
run_check() {
  local label="$1"; shift
  local logfile="$REPORT_DIR/$(echo "$label" | tr ' /' '__').log"
  echo ">>> $label"
  if "$@" >"$logfile" 2>&1; then
    pass "$label"
  else
    fail "$label (see $(basename "$logfile"))"
    tail -n 35 "$logfile" >> "$REPORT" || true
  fi
}
cleanup() {
  if [[ -n "${ORIGINAL_IME:-}" && -n "${ADB_BIN:-}" ]]; then
    "$ADB_BIN" shell ime set "$ORIGINAL_IME" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

cat > "$REPORT" <<EOF
# NovaKeys Termux Audit Report

- Started: $STARTED_AT
- Repository: $(git config --get remote.origin.url 2>/dev/null || echo unknown)
- Branch: $(git branch --show-current 2>/dev/null || echo unknown)
- Commit: $(git rev-parse HEAD 2>/dev/null || echo unknown)
- Android/Termux host: $(uname -a)
EOF

section "1. Environment"
for cmd in git java bash; do
  if command -v "$cmd" >/dev/null 2>&1; then pass "$cmd available: $(command -v "$cmd")"; else fail "$cmd missing"; fi
done
java -version >> "$REPORT" 2>&1 || true
if [[ -x ./gradlew ]]; then GRADLE=./gradlew; elif command -v gradle >/dev/null 2>&1; then GRADLE=gradle; else GRADLE=""; fi
if [[ -n "$GRADLE" ]]; then pass "Gradle runner: $GRADLE"; else fail "Gradle/gradlew not found"; fi
if [[ -n "$GRADLE" ]]; then "$GRADLE" --version > "$REPORT_DIR/gradle-version.log" 2>&1 || warn "Could not read Gradle version"; fi

section "2. Repository and source checks"
git status --short > "$REPORT_DIR/git-status.log" 2>&1 && pass "Git status captured"
git diff --check > "$REPORT_DIR/diff-check.log" 2>&1 && pass "No whitespace errors in working diff" || fail "git diff --check"
if grep -RIn --include='*.kt' --include='*.java' 'Settings\.Secure\.ENABLED_INPUT_METHODS' app/src/main  > "$REPORT_DIR/restricted-setting-scan.log" 2>&1; then
  warn "Found ENABLED_INPUT_METHODS references; inspect whether any are executable API calls or comments"
else
  pass "No ENABLED_INPUT_METHODS references in main source"
fi
if grep -RIn --include='*.kt' --include='*.java' 'TODO\|FIXME\|NotImplementedError' app/src/main > "$REPORT_DIR/todo-scan.log" 2>&1; then
  warn "TODO/FIXME markers exist in main source (inventory saved)"
else
  pass "No TODO/FIXME/NotImplementedError markers found"
fi

section "3. Android build, tests and lint"
if [[ -n "$GRADLE" ]]; then
  run_check "assembleDebug" "$GRADLE" --no-daemon assembleDebug
  run_check "assembleRelease" "$GRADLE" --no-daemon assembleRelease
  run_check "testDebugUnitTest" "$GRADLE" --no-daemon testDebugUnitTest
  run_check "lintDebug" "$GRADLE" --no-daemon lintDebug
else
  warn "Gradle tasks skipped because no Gradle runner is installed"
fi

DEBUG_APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
if [[ -s "$DEBUG_APK" ]]; then
  pass "Debug APK exists ($(du -h "$DEBUG_APK" | awk '{print $1}'))"
  if command -v aapt >/dev/null 2>&1; then
    aapt dump badging "$DEBUG_APK" > "$REPORT_DIR/apk-badging.txt" 2>&1 || warn "aapt could not inspect APK"
  elif command -v apkanalyzer >/dev/null 2>&1; then
    apkanalyzer manifest application-id "$DEBUG_APK" > "$REPORT_DIR/apk-package.txt" 2>&1 || warn "apkanalyzer could not inspect APK"
  else
    warn "aapt/apkanalyzer unavailable; APK manifest inspection skipped"
  fi
else
  fail "Debug APK missing: $DEBUG_APK"
fi

section "4. Live Android device test"
if [[ "$DEVICE_MODE" != true ]]; then
  warn "Live-device test not requested. Re-run with --device after connecting ADB over Wireless debugging."
elif ! command -v adb >/dev/null 2>&1; then
  fail "adb missing. Install with: pkg install android-tools"
elif ! adb get-state >/dev/null 2>&1; then
  fail "No ADB device. Pair/connect using Android Developer options > Wireless debugging."
elif [[ ! -s "$DEBUG_APK" ]]; then
  fail "Cannot run device test because debug APK was not built"
else
  ADB_BIN="$(command -v adb)"
  ORIGINAL_IME="$(adb shell settings get secure default_input_method 2>/dev/null | tr -d '\r')"
  adb logcat -c >/dev/null 2>&1 || true
  run_check "Install debug APK" adb install -r "$DEBUG_APK"
  IME_COMPONENT="com.creazzy.novakeys.debug/com.creazzy.novakeys.ime.NovaInputMethodService"
  HOST_COMPONENT="com.creazzy.novakeys.debug/com.creazzy.novakeys.testing.ImeSmokeTestActivity"
  if adb shell ime list -a 2>/dev/null | grep -Fq "$IME_COMPONENT"; then
    pass "NovaKeys debug IME is registered"
  else
    fail "NovaKeys debug IME is not registered in Android"
  fi
  adb shell ime enable "$IME_COMPONENT" > "$REPORT_DIR/ime-enable.log" 2>&1 && pass "IME enabled" || fail "IME enable command failed"
  adb shell ime set "$IME_COMPONENT" > "$REPORT_DIR/ime-select.log" 2>&1 && pass "NovaKeys selected as active IME" || fail "Could not select NovaKeys IME"
  adb shell am force-stop com.creazzy.novakeys.debug >/dev/null 2>&1 || true
  adb shell am start -W -n "$HOST_COMPONENT" > "$REPORT_DIR/ime-host-start.log" 2>&1 && pass "IME smoke host launched" || fail "IME smoke host failed to launch"
  sleep 8
  adb shell dumpsys input_method > "$REPORT_DIR/input-method.txt" 2>&1 || true
  adb shell dumpsys window windows > "$REPORT_DIR/windows.txt" 2>&1 || true
  adb logcat -d -v threadtime > "$REPORT_DIR/logcat.txt" 2>&1 || true
  if grep -Fq "mCurId=$IME_COMPONENT" "$REPORT_DIR/input-method.txt" && grep -Eiq 'mInputShown=true|mShowRequested=true|inputShown=true' "$REPORT_DIR/input-method.txt"; then
    pass "NovaKeys is current IME and keyboard is reported visible"
  else
    fail "IME did not reach visible state; inspect input-method.txt, windows.txt and logcat.txt"
  fi
  if grep -Eq 'FATAL EXCEPTION|Fatal signal [0-9]+|Process .*com\.creazzy\.novakeys\.debug.* has died' "$REPORT_DIR/logcat.txt"; then
    fail "Android runtime crash detected in logcat"
    grep -E -B 5 -A 20 'FATAL EXCEPTION|Fatal signal|has died' "$REPORT_DIR/logcat.txt" | tail -n 100 >> "$REPORT"
  else
    pass "No matching NovaKeys fatal crash found in captured logcat"
  fi
  adb shell ime set "$ORIGINAL_IME" >/dev/null 2>&1 || warn "Could not restore the previously selected keyboard automatically"
fi

section "5. Summary"
cat >> "$REPORT" <<EOF
- Finished: $(date -Iseconds)
- Failures: $FAILURES
- Warnings: $WARNINGS
- Full logs: $REPORT_DIR
EOF
if (( FAILURES > 0 )); then
  echo "AUDIT_RESULT=FAIL failures=$FAILURES warnings=$WARNINGS report=$REPORT"
  exit 1
else
  echo "AUDIT_RESULT=PASS failures=0 warnings=$WARNINGS report=$REPORT"
fi
