# NovaKeys Store Release Readiness

This is an engineering checklist, not a statement that the app has already passed store review. Mark each item only after verifying it.

## Automated build and artifact

- [ ] `gradle assembleDebug testDebugUnitTest lintDebug` passes on the exact release commit.
- [ ] Run on physical Android devices and supported emulator API levels; test Arabic and English IME, rotation, lifecycle recreation, autofill, image insertion, clipboard, import/export, and database migration.
- [ ] Review all lint warnings and resolve or document each one.
- [ ] Review the merged release manifest and every permission.
- [ ] Confirm the release AAB is signed with the long-term upload key and verify its certificate fingerprint.
- [ ] Keep the signing keystore and passwords out of Git. Back up the keystore securely; losing it may prevent updates unless Play App Signing is configured.
- [ ] Configure GitHub Actions repository secrets required by the `v*` release workflow:
  - `ANDROID_KEYSTORE_BASE64`: base64-encoded JKS/PKCS12 keystore
  - `ANDROID_KEYSTORE_PASSWORD`
  - `ANDROID_KEY_ALIAS`
  - `ANDROID_KEY_PASSWORD`
- [ ] Create the keystore locally with a secure password, preserve it in at least two secure backups, and never commit it or paste its passwords into issues/chat/logs.
- [ ] Increment version code for every release. Current default is only a development baseline; release workflow uses the CI run number.
- [ ] Review dependency versions, licenses, vulnerability notices, SQLCipher distribution requirements, and third-party notices.

## Product and security review

- [ ] Complete a feature-by-feature manual acceptance test. The README contains planned features; planned features must not be advertised as shipped.
- [ ] Verify that no typed text, passwords, clipboard contents, or screenshots are logged or sent over a network.
- [ ] Test password vault lock timeout, app backgrounding, biometric/device-credential fallback, SQLCipher initialization, and data migration.
- [ ] Test database recovery behavior when Android Keystore keys become unavailable (e.g., device lock reset or app data restore).
- [ ] Test CSV import against malformed, very large, and formula-injection inputs; confirm exported credentials are clearly disclosed as plaintext.
- [ ] Confirm autofill never exposes credentials in unrelated fields and handles OTP/payment fields safely.
- [ ] Review photo/media permission necessity and Google Play photo/video permissions policy. Prefer Android Photo Picker for user-selected images if broad, persistent media access is not essential.
- [ ] Add and test accessibility labels, touch target sizes, contrast, font scaling, and screen-reader navigation.
- [ ] Perform a security review of the IME, autofill service, FileProvider paths, exported components, and all intent/URI handling.
- [ ] Run automated and manual tests on Android 8.0/API 26 minimum, Android 16/API 36, and representative OEM devices.

## Google Play (new app / updates)

- [ ] Target API 36 or higher for new apps and updates under the current policy effective 31 August 2026. See [official target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878).
- [ ] Publish a real HTTPS privacy-policy URL with a monitored contact address. Review `PRIVACY.md` against the exact release code and publish it at a stable HTTPS URL before store submission; add a monitored developer contact route.
- [ ] Complete Data safety accurately from the actual shipped code and SDK inventory; do not copy claims from this checklist without verification.
- [ ] Complete app access, ads, target audience, content rating, app category, and other Play Console declarations.
- [ ] Provide app icon, feature graphic, phone/tablet screenshots as required, localized store listing, support contact, and accurate description.
- [ ] Use Play App Signing and upload the signed AAB through an internal testing track first.
- [ ] Test the Play-delivered build on real devices, review pre-launch reports/crashes/ANRs, then stage rollout.
- [ ] Check current developer-account testing requirements and policy notices in Play Console.

## Other Android stores

- [ ] Review each store's current target SDK, signing, package naming, privacy policy, content-rating, screenshots, and permission requirements separately.
- [ ] Keep package ID `com.creazzy.novakeys` stable after publication.
- [ ] Decide whether to distribute a signed APK alongside the AAB. Many stores accept APKs; Google Play primarily uses AAB for new apps.
- [ ] Maintain a signed release archive, checksum, release notes, and rollback plan for every published version.

## Known release blockers until resolved

1. A monitored developer contact route must be available before public distribution, and `PRIVACY.md` must match the exact shipped build.
2. GitHub repository signing secrets must be configured before the version-tag production workflow can publish a signed AAB.
3. The app needs physical-device acceptance testing and security/privacy review; CI build success is not equivalent to store approval.
4. The repository README documents both implemented and planned features. Store metadata must list only verified, implemented features; keep roadmap items clearly labeled.
5. Store account registration, legal ownership, app listing assets, and console declarations must be completed by the developer.


## NovaKeys-specific final smoke test

- [ ] Verify Android's keyboard switcher can switch to and away from NovaKeys; the IME subtype metadata declares switching support.
- [ ] Verify Unicode styling only changes selected text and does not modify unselected text.
- [ ] Verify sensitive-field behavior for password, visible password, web password, and numeric password variations.
- [ ] Verify image URI permissions are granted only to the intended receiving app and inaccessible after the grant is revoked.
- [ ] Confirm the published `latest` asset is clearly marked as a debug test build, never as a signed production release.
