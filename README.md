# Card Vault Android App

This app stores **cards, certifications, and titles** with two versions:
1. **Photo version** (camera capture)
2. **Digital version** (typed details)

It also supports metadata (issuer, date, details) and comments.

## What’s implemented
- Add and save items locally on device storage.
- Capture a photo using Android camera preview.
- Store typed digital details + comments.
- Tap any saved item to open a detail view with all information.

---

## How to build an APK (Android Studio)
This is the easiest path if you’re new:

1. Install **Android Studio** (Hedgehog or newer).
2. Open this project folder.
3. Wait for Gradle sync to finish.
4. Click:
   - **Build > Build Bundle(s) / APK(s) > Build APK(s)**
5. After build completes, click **locate** in Android Studio.

Typical output path:
- `app/build/outputs/apk/debug/app-debug.apk`

---

## Can I do it for you directly from this environment?
Not fully. This environment does not have open access to the Android dependency/toolchain network needed to complete a real APK build.

I added a GitHub Action workflow so you can generate APK automatically in GitHub:
- `.github/workflows/android-apk.yml`

### Build from GitHub Actions
1. Push this repo to GitHub.
2. Open **Actions** tab.
3. Run workflow: **Build Android APK**.
4. Download artifact: `card-vault-debug-apk`.

---

## Optional (terminal build on your machine)
If you have Android SDK + Gradle configured locally:

```bash
gradle assembleDebug
```

APK output:
- `app/build/outputs/apk/debug/app-debug.apk`
