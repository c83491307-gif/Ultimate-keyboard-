# NovaKeys Privacy Notice

**Last updated:** 2026-10-29

NovaKeys is an Android keyboard project. A keyboard can observe text that a user types into the focused field, so it must be treated as sensitive software. Install it only if you trust the source and understand the limitations below.

## Data handling

- **Keyboard input:** Text is entered through Android's Input Method Framework. Depending on the focused app and field, the keyboard may process keystrokes to insert them. Never assume a third-party keyboard is appropriate for passwords, payment details, recovery phrases, or other secrets without reviewing its code and testing it.
- **Local features:** The project is designed around on-device operation for keyboard entry, Unicode text styling, drawing-to-image, and locally stored image clipboard items. Local does not mean risk-free: anyone with access to an unlocked device or exported files may be able to access unencrypted data.
- **Accounts and cloud sync:** The project does not require Gmail sign-in and does not require a NovaKeys cloud account. Do not infer that every future feature is offline; check the feature documentation and network permissions in the exact build you install.
- **Password manager and autofill:** These are security-sensitive features. Do not store high-value credentials in a build that has not been independently reviewed and tested. Keep encrypted backups and their passphrases safe. CSV or other plaintext exports can expose credentials.
- **Images and files:** Images imported or created by NovaKeys may be saved in the app's private storage. If you explicitly export, share, or copy a URI, the receiving app may access the shared content under Android's URI-grant rules. Review the destination before sharing.
- **Clipboard:** Android versions and target apps control clipboard access. Clipboard behavior may vary by OS version and app. Do not expect every copied item to be captured or every image to paste in every app.
- **Diagnostics and analytics:** This repository's release process does not itself imply that app telemetry is collected. Review the exact source code and dependency list for the version you install; this notice is not an independent audit or guarantee about future versions.

## Permissions and Android controls

NovaKeys declares image/media access for supported Android versions and exposes Android's keyboard and autofill services. Android requires users to enable and select input methods and configure autofill. Grant only the permissions needed for the features you choose. You can disable NovaKeys in **Settings → System → Keyboard / Languages & input** (wording varies by device) and revoke media access in Android app settings.

## Retention and deletion

Data saved in app-private storage may remain until deleted by the app, Android settings, or uninstalling the app. Uninstalling typically removes app-private data, but exported files, shared copies, backups, and files saved to user-selected folders may remain and must be removed separately. Keep a backup before clearing data or uninstalling if you need to preserve local content.

## Children and third parties

NovaKeys is not designed to transmit user data to a service operated by the project. The keyboard interacts with third-party apps where you type or share content; those apps' own privacy terms apply. This project does not control their handling of information.

## Changes and contact

This notice may change as features are added. For privacy questions or to report a concern, use the repository's issue tracker: https://github.com/c83491307-gif/Ultimate-keyboard-/issues. Do not post passwords, private keys, personal data, or other secrets in a public issue.

**Before public distribution:** The maintainer should review this notice against the exact release build, add a monitored contact address if appropriate, and complete any required app-store Data safety and privacy disclosures. This document is not legal advice.
