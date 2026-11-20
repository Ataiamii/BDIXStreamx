# BDIXStream Android App

A lightweight, CloudStream-inspired Android streaming app for BDIX FTP servers (**Circle FTP**, **Sam Online**, **ICC FTP**, and custom **GitHub `repo.json` extensions**).

## Key Features
- **Native CORS & HTTP Bypass:** Built-in Android `BDIXNative` bridge fetches directly from local HTTP BDIX servers (`http://172.16.50.4`, `http://circleftp.net`, etc.) without browser CORS blocks.
- **GitHub Extension Manager:** Add or update BDIX servers anytime by pasting a GitHub `repo.json` URL — no need to rebuild the app when ISP IPs change.
- **Built-in Player + Native External Players:** Watch MP4/HLS streams inside the app with fullscreen auto-rotation and subtitle upload (`.vtt`/`.srt`), or tap **Open in VLC / MX Player** for MKV & dual-audio support.

## How to Build the `.apk` on GitHub (No PC Required!)
1. Create a new repository on [GitHub](https://github.com/new) (e.g., `BDIXStream`).
2. Extract `BDIXStream-Android-App.zip` and upload all files & folders (including `.github/workflows/build-apk.yml`) to your GitHub repository.
   - **Tip (Phone or PC):** If using Git locally, run:
     ```bash
     git init
     git add .
     git commit -m "Initial BDIXStream Android app"
     git branch -M main
     git remote add origin https://github.com/YOUR_USERNAME/BDIXStream.git
     git push -u origin main
     ```
3. Open your GitHub repository and tap the **Actions** tab.
4. Click the **Build BDIXStream Android APK** workflow (wait ~2 minutes for the green checkmark).
5. Scroll down to **Artifacts** and download **BDIXStream-APK**.
6. Unzip it and install `app-debug.apk` on your Android phone!

## Using Your Own GitHub Repo for Extensions
This repository includes a starter `repo.json` at the root. Edit `repo.json` on GitHub whenever you want to add new BDIX servers, then paste its GitHub URL into the **Extensions** tab inside the app.
