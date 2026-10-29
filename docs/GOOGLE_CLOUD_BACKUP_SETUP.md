# Google sign-in and encrypted cloud backup setup

The app does not include the owner's Firebase credentials. Cloud sign-in is disabled until these settings are supplied.

1. Create a Firebase project and register Android package `com.creazzy.novakeys`.
2. Enable Google under Firebase Authentication sign-in providers.
3. Add SHA-1 and SHA-256 fingerprints for debug and release signing certificates.
4. Create/use a **Web application OAuth client ID** in Google Cloud Credentials.
5. Enable Cloud Firestore.
6. Configure GitHub Actions secrets (or local environment variables): `FIREBASE_API_KEY`, `FIREBASE_APP_ID`, `FIREBASE_PROJECT_ID`, `GOOGLE_WEB_CLIENT_ID`.
7. Deploy and test these Firestore rules:

```text
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

Cloud documents contain an encrypted backup envelope, not readable vault entries. AES-GCM encryption uses a passphrase entered by the user and derived locally with PBKDF2-HMAC-SHA256. The passphrase is never uploaded or saved by NovaKeys. Keep it safe: it cannot be recovered if lost.

Before production: test cross-account isolation, wrong-passphrase handling, account switching, network failures, Firestore rules, certificate fingerprints, and revise the privacy policy to disclose Firebase Authentication and Firestore metadata.
