# NovaKeys Privacy Policy

**Effective date:** 29 October 2026  
**App:** NovaKeys for Android  
**Developer contact:** REPLACE_BEFORE_PUBLICATION_WITH_A_MONITORED_EMAIL_ADDRESS

> Publication blocker: replace the contact placeholder with a real monitored email address, host this policy at a stable public HTTPS URL, and enter that URL in each store console before release.

## Summary

NovaKeys is an Android keyboard with a local password vault, phone-storage backup, and clipboard/image utilities. A keyboard can handle text typed into other apps. The app does not require email sign-in and does not use Firebase or cloud backup. The app declares no advertising or analytics SDKs.

## Information processed

Depending on the features a user chooses, NovaKeys may process:

- **Text typed through the keyboard:** Android delivers input to the currently focused app. NovaKeys must not collect or transmit typed text.
- **Password vault entries:** labels, usernames, passwords, websites, folders and timestamps. The local vault uses SQLCipher; its database key is protected using Android Keystore.
- **Clipboard and images:** images a user imports or saves to the NovaKeys image clipboard. If screenshot auto-sync is enabled and media access is granted, the app may scan accessible media for new screenshots while its keyboard service runs.
- **CSV files:** CSV imports and exports. CSV exports contain readable credentials and are not encrypted by the CSV format.
- **Encrypted backup files:** backup files are encrypted on-device using AES-GCM with a key derived from a separate passphrase the user enters. NovaKeys does not save or upload that passphrase.
- **Local phone files:** when the user chooses a storage folder and imports files, NovaKeys copies the selected files into a `NovaKeys` subfolder in that location. File access is mediated by Android's Storage Access Framework.

All storage and backup features are local. No email or Google account is required.

## How information is used

Information is used to provide the feature invoked by the user: text entry, local credential storage and autofill, image/clipboard management, and local import/export. Files are copied only when the user explicitly selects them.

## Storage and security

Vault records are stored in a local SQLCipher database, with the database key protected using Android Keystore. Backup files are encrypted locally using AES-GCM with a key derived from a separate passphrase. The passphrase is never stored by NovaKeys. If the user loses that passphrase, the encrypted backup cannot be decrypted. Android Keystore and device security reduce exposure but cannot guarantee protection against every compromised device, malware, or rooted-device attack.

CSV exports contain readable secrets. Protect and delete them after use. Android backup is disabled in the app manifest; uninstalling or clearing app data may permanently remove local records.

## Sharing and sale

NovaKeys does not include advertising or analytics SDKs and does not upload vault entries or phone files to a cloud service. No sale of user data is intended.

## Permissions

Depending on Android version and feature use, NovaKeys may request image/media access for specific features. Phone-folder and file import/export use Android's Storage Access Framework so users choose which files and folders the app can access. The keyboard and autofill services are declared so Android can offer those system functions. Users can review or revoke permissions in Android Settings.

Grant only permissions needed for features you intend to use. Do not enable NovaKeys as the default keyboard unless you trust and understand its behavior.

## Children

NovaKeys is not specifically designed for children. Store distribution and age-rating declarations must be completed by the developer in each store console.

## Data retention and deletion

Local records can be deleted using available app controls, by clearing app storage, or by uninstalling the app. Files copied into the user-selected `NovaKeys` folder and exported files may remain on the phone after uninstalling and must be deleted separately by the user.

## Changes to this policy

This policy must be reviewed and updated whenever app behavior, permissions, dependencies, or data handling changes.

## Contact

For privacy questions or requests, contact: **REPLACE_BEFORE_PUBLICATION_WITH_A_MONITORED_EMAIL_ADDRESS**.
