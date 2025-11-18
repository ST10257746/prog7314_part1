# Play Store Preparation Guide

This guide outlines the steps and assets required to publish FitTrackr on the Google Play Store.

## Prerequisites

- Google Play Console account (one-time $25 registration fee)
- Signed release APK
- App assets (screenshots, feature graphic, app icon)
- Privacy policy URL
- Content rating questionnaire completed

## Step 1: Generate Signed Release APK

### Option A: Using Android Studio

1. Open the project in Android Studio
2. Go to **Build** → **Generate Signed Bundle / APK**
3. Select **APK**
4. Choose your keystore file (create one if needed)
5. Enter keystore password and key alias
6. Select **release** build variant
7. Click **Finish**

The signed APK will be generated at: `app/build/outputs/apk/release/app-release.apk`

### Option B: Using Command Line

```bash
# Create keystore (if not exists)
keytool -genkey -v -keystore fittrackr-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias fittrackr

# Build release APK
./gradlew assembleRelease

# Sign the APK
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 -keystore fittrackr-release-key.jks app/build/outputs/apk/release/app-release-unsigned.apk fittrackr

# Zipalign (optional but recommended)
zipalign -v 4 app/build/outputs/apk/release/app-release-unsigned.apk app/build/outputs/apk/release/app-release.apk
```

## Step 2: Prepare App Assets

### Required Screenshots

**Phone Screenshots (1080x1920px minimum):**
- At least 2 screenshots required
- Recommended: 4-8 screenshots showing key features
- Show: Login, Home Dashboard, Workout Tracking, Progress Charts, Nutrition Tracking, Settings

**Tablet Screenshots (optional but recommended):**
- 7-inch tablets: 1200x1920px
- 10-inch tablets: 1600x2560px

### Feature Graphic

- **Size:** 1024x500px
- **Format:** PNG or JPG
- **Content:** App name, tagline, key visual elements
- **Text:** Keep text minimal (20% of image max)

### App Icon

- **Size:** 512x512px
- **Format:** PNG (32-bit with alpha channel)
- **Location:** Already configured in `app/src/main/res/mipmap-*/ic_launcher.png`

## Step 3: Play Console Setup

1. **Create App Listing:**
   - Go to Google Play Console
   - Click "Create app"
   - Fill in app details:
     - App name: FitTrackr
     - Default language: English
     - App or game: App
     - Free or paid: Free

2. **Complete Store Listing:**
   - Short description (80 characters max)
   - Full description (4000 characters max)
   - App icon (512x512px)
   - Feature graphic (1024x500px)
   - Screenshots (phone and tablet)
   - Category: Health & Fitness
   - Content rating: Complete questionnaire

3. **Set Up Release:**
   - Go to "Production" → "Create new release"
   - Upload signed APK or AAB
   - Add release notes
   - Review and roll out

## Step 4: Content Rating

Complete the content rating questionnaire:
- Age group: 13+
- Content descriptors: None required for fitness app
- Data safety: Complete data safety section

## Step 5: Privacy Policy

Required for apps that collect user data. Include:
- What data is collected
- How data is used
- Data storage and security
- User rights

Host privacy policy on a publicly accessible URL and add to Play Console.

## Step 6: Testing

Before publishing:
- Test on multiple devices
- Test all features (login, sync, offline mode, notifications)
- Verify app works on different Android versions
- Test in different languages

## Step 7: Release

1. **Internal Testing:**
   - Upload to internal testing track first
   - Test with team members
   - Fix any critical issues

2. **Alpha/Beta Testing:**
   - Release to closed alpha/beta testing
   - Gather feedback
   - Make improvements

3. **Production Release:**
   - Once satisfied, release to production
   - Monitor reviews and ratings
   - Respond to user feedback

## Checklist

- [ ] Signed release APK generated
- [ ] App icon (512x512px) prepared
- [ ] Feature graphic (1024x500px) created
- [ ] Phone screenshots (at least 2) taken
- [ ] Tablet screenshots (optional) taken
- [ ] App description written
- [ ] Release notes prepared
- [ ] Privacy policy URL added
- [ ] Content rating completed
- [ ] Data safety section completed
- [ ] App tested on multiple devices
- [ ] All features verified working

## Resources

- [Google Play Console](https://play.google.com/console)
- [Play Console Help](https://support.google.com/googleplay/android-developer)
- [App Asset Guidelines](https://developer.android.com/distribute/googleplay/promote/brand)
- [Content Rating](https://support.google.com/googleplay/android-developer/answer/9888179)

## Notes

- App signing by Google Play is recommended (automatic key management)
- First release may take 1-7 days for review
- Updates typically reviewed within 1-3 days
- Keep release notes updated with each version

