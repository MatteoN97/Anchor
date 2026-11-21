# Anchor

A coping companion for people in therapy, for Android and iOS.

Anchor is built around one question: what do you reach for when things get bad, at the moment
you are least able to think clearly? The app keeps the answer ready in advance.

- **One button when it counts.** The home screen is mostly empty on purpose. *I need help now*
  starts a guided flow: it asks what is actually going on, then routes accordingly — paced
  breathing when you are anxious, straight to grounding when you are angry or exhausted and
  being told to breathe would only irritate.
- **A toolkit that is yours.** Coping strategies you have found to work, in your own words.
  When the toolkit is thin, Anchor falls back on built-in techniques matched to how you feel,
  and offers to save the ones that helped.
- **A therapy journal.** Notes with tags, and questions you want to raise in the next session,
  so the thought you had on a Tuesday is still there on Thursday.
- **A check-in afterwards.** Anchor asks how you feel now. If the answer is "still rough" it
  does not congratulate you — it offers another round, or the number of a crisis line.

Everything stays on the device. There is no account, no network call, no analytics.

## Architecture

Kotlin Multiplatform with Compose Multiplatform: one codebase, two native apps.

| Module       | Contents                                                                      |
|--------------|-------------------------------------------------------------------------------|
| `shared`     | Models, Room database, ViewModel and the entire Compose UI. Builds for Android and iOS. |
| `androidApp` | Android application shell — `MainActivity`, launcher icons, manifest.           |
| `iosApp`     | Xcode project hosting the shared UI in a `UIViewControllerRepresentable`.       |

Essentially all application code lives in `shared/src/commonMain`, UI included. Only five things
differ per platform, behind `expect`/`actual` in `shared/src/{androidMain,iosMain}`: the database
file location, preference storage, the phone dialer, Material You colours, and the clock.

Persistence is Room with the bundled SQLite driver, which compiles to both targets.

## Building

### Android

    ./gradlew :androidApp:installDebug

### iOS

Open `iosApp/iosApp.xcodeproj` in Xcode and run. A build phase invokes Gradle to produce and embed
the `Shared` framework, so there is no separate Gradle step.

The iOS simulator target requires Apple Silicon: `iosX64` is not built, because neither Compose
Multiplatform 1.12 nor `androidx.sqlite:sqlite-bundled` publishes that target any more.

## Requirements

- Android 7.0 (API 24) or newer
- iOS 15.0 or newer
- JDK 21 and Xcode 16 or newer to build

## A note on scope

Anchor is not a therapy app and does not try to be one. It holds no advice and makes no
assessment — it is a place to put what already works for you, arranged so you can reach it
one-handed on a bad day. The crisis line it offers in Switzerland is
[Die Dargebotene Hand / La Main Tendue](https://www.143.ch), reachable on **143**.
