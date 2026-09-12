# Custom Recipebook

A Kotlin + Jetpack Compose cookbook for Android. Keep family recipes in a warm terracotta-and-sand book: import a PDF, snap a page, or type a recipe by hand. Ingredient lines toggle as a checklist, US/Metric units sit side by side, and a batch scaler multiplies every quantity.

## What’s in v1

- **Recipes home** — Custom Recipebook header, PDF / Camera / Manual chips, All · Dinner · Baking · Saved filters, vertical recipe cards
- **Add recipe** — upload a PDF, take or choose a photo, or enter a recipe manually. PDF/photo parsing is **stubbed** with a sample OCR preview (Creamy White Bean Soup + confidence bars) you can review and save
- **Recipe detail** — optional hero photo, US ↔ Metric toggle (cups and grams on every line), servings + batch scale (− / N× / +), tappable ingredient checklist, numbered directions in larger type
- **Seeded book** — Chocolate Chip Cookies, One-Pan Lemon Chicken, Garlic & Herb Pasta, Simple Seeded Bread, Morning Banana Muffins
- **Shopping & Settings** — shopping is a placeholder that lists ingredients you’ve checked off; settings stores the default unit system and can clear checklists
- **Local persistence** — Room for recipes and checklist state, DataStore for the unit preference. No account or backend.

## Requirements

- Android Studio Ladybug / Koala or newer (or command-line SDK)
- JDK 17+
- Android SDK 35, Build-Tools 35.0.0
- minSdk 26, targetSdk 35

## Run locally

```bash
# Point Gradle at your SDK (Android Studio does this for you)
echo "sdk.dir=$ANDROID_HOME" > local.properties

./gradlew :app:assembleDebug
```

Install the debug APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

A copy of the last successful debug build is also written to [`dist/CustomRecipebook-debug.apk`](dist/CustomRecipebook-debug.apk) after `./gradlew :app:assembleDebug`.

Open the project folder in Android Studio and run the **app** configuration on a phone or emulator (portrait).

## Package

`com.customrecipebook.app` (debug builds use `.debug` suffix)

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

## Notes

- Camera and storage access are used only when you choose those import paths.
- Real OCR / PDF text extraction is intentionally out of scope for v1; the preview is a realistic stub so the rest of the book can be used immediately.
- Chocolate Chip Cookies ships with a bundled hero photo so the detail screen matches the intended layout without an import.
