# Custom Recipebook

A Kotlin + Jetpack Compose cookbook for Android. Keep family recipes in a warm terracotta-and-sand book: import a PDF, snap a page, or type a recipe by hand. Ingredient lines toggle as a checklist, US/Metric units sit side by side, and a batch scaler multiplies every quantity.

The book ships **empty**. Everything on Home comes from a PDF, a photo, or a recipe you type.

## What’s in v1

- **Recipes home** — Custom Recipebook header, PDF / Camera / Manual chips, All · Dinner · Baking · Saved filters. Empty state nudges those three import paths.
- **Add recipe** — pick a PDF (`application/pdf` document picker), take or choose a photo, or enter a recipe manually.
- **PDF import** — copies the file into app storage, reads a text layer when one exists, prefills title (from the filename or the first line), ingredients, and steps, then opens an editable Review form. If extract fails, the PDF is still attached and you fill the form.
- **Recipe detail** — optional hero photo, US ↔ Metric toggle, servings + batch scale (− / N× / +), tappable ingredient checklist, numbered directions. PDF recipes can reopen the attached file.
- **Shopping & Settings** — shopping lists ingredients you’ve checked off; settings stores the default unit system and can clear checklists.
- **Local persistence** — Room for recipes, attachments, and checklist state; DataStore for the unit preference. No account or backend.

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

A copy of the last successful debug build is also written to [`dist/CustomRecipebook-debug.apk`](dist/CustomRecipebook-debug.apk).

Open the project folder in Android Studio and run the **app** configuration on a phone or emulator (portrait).

## Package

`com.customrecipebook.app` (debug builds use `.debug` suffix)

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

## Notes

- Camera and storage access are used only when you choose those import paths.
- PDF text comes from the file’s text layer (content streams). Scanned image-only PDFs still import as an attached file with an editable blank recipe.
- Camera import attaches the photo; it does not run OCR in v1.
