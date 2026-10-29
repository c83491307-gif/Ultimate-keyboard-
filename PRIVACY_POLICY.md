# NovaKeys Privacy Policy

**Effective date:** 29 October 2026  
**App:** NovaKeys for Android  
**Developer contact:** REPLACE_BEFORE_PUBLICATION_WITH_A_MONITORED_EMAIL_ADDRESS

> Publication blocker: replace the contact placeholder with a real monitored email address, host this policy at a stable public HTTPS URL, and enter that URL in each store console before release.

## Summary

NovaKeys is an Android keyboard with a local password vault and clipboard/image utilities. A keyboard can handle text typed into other apps. NovaKeys includes optional Google sign-in and encrypted cloud-backup code; these cloud features remain unavailable until the app owner configures Firebase Authentication and Cloud Firestore. The app declares no advertising or analytics SDKs.

## Information processed

Depending on the features a user chooses, NovaKeys may process:

- **Text typed through the keyboard:** Android delivers input to the currently focused app. NovaKeys must not collect or transmit typed text.
- **Password vault entries:** labels, usernames, passwords, websites, folders and timestamps. The local vault uses SQLCipher; its database key is protected using Android Keystore.
- **Clipboard and images:** images a user imports or saves to the NovaKeys image clipboard. If screenshot auto-sync is enabled and media access is granted, the app may scan accessible media for new screenshots while its keyboard service runs.
- **CSV files:** CSV imports and exports. CSV exports contain readable credentials and are not encrypted by the CSV format.
- **Encrypted backup files:** backup files are encrypted on-device using AES-GCM with a key derived from a separate passphrase the user enters. NovaKeys does not save or upload that passphrase.
- **Google account information (optional):** when a user chooses Google sign-in, Firebase Authentication processes the account identifier and basic profile details needed to authenticate the session.
- **Cloud backup (optional):** when a user signs in and explicitly saves a backup, NovaKeys uploads an encrypted backup envelope to Cloud Firestore under that user's Firebase account ID. Firestore may process document metadata such as update timestamps. Vault entries are encrypted on the device before upload.

Local features and phone-file backups do not require a Google account. Google sign-in is optional and is used for cloud backup.

## How information is used

Information is used to provide the feature invoked by the user: text entry, local credential storage and autofill, image/clipboard management, import/export, and optional encrypted cloud backup. Cloud backup is uploaded only after the user signs in and explicitly chooses the save action.

## Storage and security

Vault records are stored in a local SQLCipher database, with the database key protected using Android Keystore. Optional cloud backup content is encrypted locally using AES-GCM before upload. The backup passphrase is never uploaded or stored by NovaKeys. If the user loses that passphrase, the encrypted backup cannot be decrypted. Android Keystore and device security reduce exposure but cannot guarantee protection against every compromised device, malware, or rooted-device attack.

CSV exports contain readable secrets. Protect and delete them after use. Android backup is disabled in the app manifest; uninstalling or clearing app data may permanently remove local records.

## Sharing and sale

NovaKeys does not include advertising or analytics SDKs. When cloud backup is configured and used, Firebase Authentication and Cloud Firestore process account and backup metadata. The backup payload is encrypted before upload. No sale of user data is intended.

## Permissions

Depending on Android version and feature use, NovaKeys may request image/media access to import images and optionally monitor screenshots. Internet access is used by the optional Google sign-in and cloud backup features. The keyboard and autofill services are declared so Android can offer those system functions. Users can review or revoke permissions in Android Settings.

Grant only permissions needed for features you intend to use. Do not enable NovaKeys as the default keyboard unless you trust and understand its behavior.

## Children

NovaKeys is not specifically designed for children. Store distribution and age-rating declarations must be completed by the developer in each store console.

## Data retention and deletion

Local records can be deleted using available app controls, by clearing app storage, or by uninstalling the app. Exported files may remain on the phone or in other apps. The current cloud implementation stores one encrypted backup document per signed-in Firebase user. Until an in-app remote-delete control is added, the developer or user must delete that document through the configured Firebase project. Firebase account and service retention are also subject to the project's settings and applicable Firebase terms.

## Changes to this policy

This policy must be reviewed and updated whenever app behavior, permissions, dependencies, or data handling changes.

## Contact

For privacy questions or requests, contact: **REPLACE_BEFORE_PUBLICATION_WITH_A_MONITORED_EMAIL_ADDRESS**.
