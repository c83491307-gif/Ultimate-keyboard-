# Security Policy

## Scope and status

NovaKeys is an Android keyboard and includes password-manager/autofill-related code. Treat all releases as **beta until the relevant security review and physical-device testing are complete**. A successful CI build proves build/test/lint steps passed; it is not a security audit.

## Supported versions

Security fixes target the latest commit on `main` and the latest tagged release, if any. Automated `latest` downloads are debug test builds and must not be treated as production-signed releases.

## Reporting a vulnerability

Please do not publish exploit details, credentials, backup files, or personally identifiable information in a public issue. If GitHub private vulnerability reporting is enabled for this repository, use that channel. Otherwise contact the maintainer through the GitHub profile and ask for a private reporting route before sending technical details.

Include the affected commit or version, Android version/device where relevant, reproducible steps, expected impact, and a minimal proof of concept. Allow time for investigation and a fix before public disclosure.

## Maintainer release checklist

- Keep the release keystore and passwords outside Git; use repository secrets or a secure release environment.
- Back up the keystore securely. Losing it can prevent updates signed as the same app.
- Never commit signing keys, passwords, API tokens, real password-manager data, or decrypted backups.
- Review exported components, FileProvider paths, URI grants, permissions, and dependencies before every release.
- Test password fields, numeric PIN fields, autofill, clipboard, image sharing, backup import/export, Arabic/English input, and keyboard switching on physical devices.
- Verify release APK/AAB signatures and publish SHA-256 checksums.
- Do not call a build production-ready until a signed release and device-level acceptance tests are complete.
