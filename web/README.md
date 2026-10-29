# NovaKeys Web

A mobile-first, static web companion for the NovaKeys Android keyboard. It provides local Unicode text styling, copy-to-clipboard, TXT export, and direct links to the Android test APK.

## Run locally
Open `web/index.html` in a browser. For PWA/offline caching, serve the `web/` directory over HTTPS or localhost.

## Deploy
The website is static and can be hosted by Vercel, GitHub Pages, or any static host. Set the root directory to `web`; no build command is required and the output directory is `.`.

## Privacy
Text transformation is performed in the browser; the web tool does not call a text API. See [PRIVACY_POLICY.md](../PRIVACY.md).

## Android download
The website links to the current GitHub prerelease APK. It is explicitly labelled a debug/test build, not a production-signed release: https://github.com/c83491307-gif/Ultimate-keyboard-/releases/tag/latest


## Install as a web app
The page includes a web manifest, SVG app icon, and service worker. On supported Android browsers, use the in-page install action when it appears; otherwise open the browser menu and choose **Install app** or **Add to Home screen**. The app shell is cached after a successful first visit.
