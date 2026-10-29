# NovaKeys Privacy Policy

**Effective date:** 29 October 2026  
**App:** NovaKeys for Android  
**Developer contact:** REPLACE_BEFORE_PUBLICATION_WITH_A_MONITORED_EMAIL_ADDRESS

> Publication blocker: replace the contact placeholder above with a real monitored contact address before publishing this policy or submitting the app to any store. Host this page at a stable public HTTPS URL and enter that URL in every store console.

## Summary

NovaKeys is an Android keyboard with a local password vault and clipboard/image utilities. A keyboard can handle text that users type, including sensitive text. The current source manifest does not request the Internet permission, and the declared app dependencies do not include advertising or analytics SDKs. The app is designed to process its core functions on the device.

Do not publish this policy unchanged if the app later adds network access, cloud sync, third-party AI, advertising, analytics, crash reporting, or other data-sharing behavior.

## Information processed

Depending on the features a user chooses to use, NovaKeys may process:

- **Text typed through the keyboard:** Android delivers text input to the currently focused app. NovaKeys must not collect or transmit that text. The keyboard UI suppresses some extra tools for detected sensitive fields, but this does not replace careful user judgment.
- **Password vault entries:** labels, usernames, passwords, websites, folders, and timestamps entered by the user. The vault database is configured to use SQLCipher; its database passphrase is wrapped with Android Keystore.
- **Clipboard and images:** images the user imports or saves to the NovaKeys image clipboard. If the user enables screenshot auto-sync and grants the required photo access, the app scans accessible media for new screenshots while the keyboard service is running.
- **Files the user exports or imports:** for example, password CSV files. Exported CSV files contain readable credentials and are not encrypted by the CSV format; protect and delete them after use.

NovaKeys does not require an account to use the current local features.

## How information is used

Information is used only to provide the feature the user invokes: text entry, local credential storage and autofill, image/clipboard management, and user-requested import/export. The current build does not declare Internet access. No remote account, cloud synchronization, advertising, or analytics feature is documented as part of the current app.

## Storage and security

Vault records are stored in a local SQLCipher database, with the database key protected using Android Keystore. Android Keystore and device security reduce exposure but cannot guarantee protection against every compromised device, malware, rooted-device attack, or user-created export. CSV exports are not encrypted by NovaKeys. The app disables Android backup in its manifest; uninstalling or clearing app data can permanently remove local records.

Do not use the app as the only copy of important credentials. Keep exports secure and do not share them through untrusted channels.

## Sharing and sale

The current source declares no network permission and no advertising or analytics SDK dependency. The app is not designed to sell user data or share local vault entries with an external service. Reassess this statement before adding any network-enabled feature or third-party SDK.

## Permissions

Depending on Android version and feature usage, NovaKeys may request access to images/media to import images and optionally monitor screenshots. The keyboard and autofill services are declared so Android can offer those system functions. Users can review or revoke permissions in Android Settings; some features will then be unavailable.

Grant only permissions needed for features you intend to use. Do not enable NovaKeys as the default keyboard unless you trust and understand its behavior.

## Children

NovaKeys is not specifically designed for children. Store distribution and age-rating declarations must be completed by the developer in each store console.

## Data retention and deletion

Data stays in app-private local storage unless the user exports it. Users can delete saved entries and images through the app's available controls, clear app storage, or uninstall the app. Exported files may remain in Downloads, cloud backups, or other apps and must be deleted separately by the user.

## Changes to this policy

This policy must be updated whenever app behavior, permissions, dependencies, or data handling changes. The effective date should be revised for material changes.

## Contact

For privacy questions or requests, contact: **REPLACE_BEFORE_PUBLICATION_WITH_A_MONITORED_EMAIL_ADDRESS**.
