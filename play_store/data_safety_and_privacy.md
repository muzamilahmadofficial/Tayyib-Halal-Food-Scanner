# Google Play Data Safety, Privacy Policy & Publishing Checklist — Tayyib

## 1. Google Play Data Safety Form Answers (Exact Match to Implemented Code)
- **Does your app collect or share any of the required user data types?** **No.**
  - No account creation, login, or authentication SDKs.
  - No analytics, advertising, or crash-tracking third-party SDKs.
  - Camera frames are processed strictly on-device via CameraX, Google ML Kit Barcode Scanning (`com.google.mlkit:barcode-scanning`), and Google ML Kit Text Recognition (`com.google.mlkit:text-recognition`) — photos and camera frames are never uploaded or stored.
  - Photo selection for Lens OCR uses Android's zero-permission Photo Picker (`ActivityResultContracts.PickVisualMedia`).
  - Scan history, favourites, cached product JSON, and user preferences reside strictly locally on-device in Room (`tayyib_local.db`) and DataStore (`tayyib_settings`).
  - Outbound HTTPS requests are sent only to `https://halalopenfoodfacts.org/` (primary) and `https://world.openfoodfacts.org/` (automatic backup) containing only barcode or product search query strings; no personal identifiers or device ad IDs are transmitted.
- **Is all data encrypted in transit?** **Yes** (HTTPS enforced with `android:usesCleartextTraffic="false"`).
- **Permissions Declared in `AndroidManifest.xml`**:
  - `android.permission.CAMERA` (runtime-requested strictly for barcode scanning and Lens label OCR; optional hardware feature)
  - `android.permission.INTERNET` & `android.permission.ACCESS_NETWORK_STATE` (product lookup, search, and alternatives)
  - `android.permission.VIBRATE` (optional haptic feedback toggleable in Settings)
  - Zero storage permissions (`READ_EXTERNAL_STORAGE` / `READ_MEDIA_IMAGES` not declared), zero location permissions, zero contacts/phone permissions.

## 2. Ready-to-Host Privacy Policy URL
A ready-to-publish standalone bilingual (English & French) HTML Privacy Policy is included at:
- `play_store/privacy_policy.html`

Upload `privacy_policy.html` to GitHub Pages, Netlify, Cloudflare Pages, or your personal HTTPS site and paste that public URL into **Google Play Console → App content → Privacy policy**.

## 3. Ready-to-Upload Store Graphics (`play_store/assets/`)
- **App Icon (512×512 PNG)**: `play_store/assets/tayyib_app_icon_512.png`
- **Feature Graphic (1024×500 PNG)**: `play_store/assets/tayyib_feature_graphic_1024x500.png`
- **Promo Banner (1920×1080 PNG)**: `play_store/assets/tayyib_promo_banner_1920x1080.png`
- **Web Favicon (64×64 PNG)**: `play_store/assets/tayyib_favicon_64.png`
- **Phone Screenshots (Required: 2–8 screenshots, 16:9 or 9:16, min 320px / max 3840px)**:
  Capture real screenshots directly from the AI Studio Streaming Emulator or a physical Android phone running Tayyib covering:
  1. Home screen (with Live Halal Products carousel & EN|FR toggle)
  2. Barcode Scanner viewfinder
  3. Halal product result
  4. Haram product result (showing inline Suggested Halal Alternatives)
  5. Doubtful product result & E-number breakdown
  6. Ingredient Checker (with Lens Camera & From Photo OCR)
  7. Halal Alternatives explorer
  8. French interface (`FR`)

## 4. Generating Your Signed Release `.aab` & Publishing to Google Play Console
Google Play requires an `.aab` signed with **your private upload keystore** (never committed to source control):

1. **Export the Android project** (via AI Studio Export ZIP or Push to GitHub) and open it in **Android Studio** (or use a local terminal with JDK + Gradle).
2. **Generate your private upload keystore** (keep it backed up safely!):
   ```bash
   keytool -genkey -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
   ```
3. **Build the signed release Android App Bundle (`.aab`)**:
   - **Option A (Android Studio GUI)**: Go to **Build → Generate Signed App Bundle / APK → Android App Bundle**, select `my-upload-key.jks`, alias `upload`, and select the `release` build variant.
   - **Option B (CLI using `app/build.gradle.kts` release signing config)**:
     ```bash
     export KEYSTORE_PATH=/secure/path/to/my-upload-key.jks
     export STORE_PASSWORD="<your-keystore-password>"
     export KEY_PASSWORD="<your-key-password>"
     ./gradlew :app:bundleRelease
     ```
   - Output file: `app/build/outputs/bundle/release/app-release.aab`
4. **Publish in Google Play Console** (`https://play.google.com/console`):
   - Create a new app (`com.muzamilahmad.tayyib`), default language English (en-US), and add French (fr-FR) translation.
   - Paste copy from `play_store/store_listing_en.md` and `play_store/store_listing_fr.md`.
   - Upload `tayyib_app_icon_512.png`, `tayyib_feature_graphic_1024x500.png`, and your real phone screenshots.
   - Complete **App content**: Privacy Policy URL (from `privacy_policy.html`), Data Safety (answers in Section 1 above), Ads declaration (**No, my app does not contain ads**), Content Rating (IARC -> Everyone / PEGI 3), Target Audience, and Government/News/Financial declarations (No).
   - Upload `app-release.aab` to **Internal testing** (or **Closed testing** for personal developer accounts), test on real Android phones, then submit for **Production** review.

## 5. Developer Contact & Attribution
- **Developer**: Muzamil Ahmad (Independent Developer)
- **Email**: Muzamilofficially@gmail.com
- **LinkedIn**: https://www.linkedin.com/in/muzamilahmadofficial
- **Data Sources**: Halal Open Food Facts (`https://halalopenfoodfacts.org/en/`) & Open Food Facts (`https://world.openfoodfacts.org/`, ODbL v1.0)
