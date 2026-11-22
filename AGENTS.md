# AGENTS.md — onboarding for coding agents

Read this before changing anything. It is the short, rule-shaped version of the architecture in
[`README.md`](README.md) (see "Architecture at a glance" and "Architectural decisions"). Where a
rule here looks arbitrary, the README's decision table (D1–D12) says why.

## What this project is

Anchor: a coping companion for people in therapy. Android + iOS, **fully offline**, one Kotlin
Multiplatform codebase. Home with an SOS button → crisis flow (feeling → breathing → suggested
tools → check-in); toolkit of the user's own coping tools; therapy journal; mood tracker.

## Where code goes

| You are adding… | Put it in |
|-----------------|-----------|
| Anything not platform-specific (models, logic, UI, screens) | `shared/src/commonMain/kotlin/ch/bfh/anchor/` |
| Pure domain logic | `model/` — and a test in `shared/src/commonTest/` |
| A table, DAO or query | `data/` |
| State or an action the UI triggers | `viewmodel/AnchorViewModel.kt` |
| A preference | `settings/SettingsManager.kt`, exposed as a `StateFlow` with a setter |
| A screen | `ui/<feature>/`, built from `ui/adaptive/` wrappers |
| A user-visible string | `shared/src/commonMain/composeResources/values/strings.xml` **and** `values-de`, `values-fr`, `values-it` |
| An icon | path data in `ui/icons/AnchorIcons.kt` |
| Something with no common API | `expect` in `commonMain/platform/`, `actual` in `androidMain` and `iosMain` |
| Android-only app wiring | `androidApp/` (keep it thin) |

## Rules

**Must**

- Use the `Anchor*` / `Adaptive*` wrappers from `ui/adaptive/` (`AnchorButton`,
  `AdaptivePageScaffold`, `AdaptiveSheet`, `AdaptiveTextField`, `AdaptiveSwitch`,
  `adaptiveSection`, …). They render Material on Android and an iOS look on iOS.
- Read colours from `MaterialTheme.colorScheme`; `AnchorTheme` fills it per platform.
- Decide platform behaviour with `LocalIsIos.current`, never `isIosHost` directly.
- Pass `AnchorViewModel` and `SettingsManager` as plain parameters. One instance of each is
  created at the platform entry points (`MainActivity`, `MainViewController`).
- Store **stable keys**, render translated labels. Tool categories are `ToolCategory.key`; the
  crisis router (`Feeling.matchingCategories`) matches on keys.
- Use positional format arguments in strings (`%1$d`, not `%d`) and plain apostrophes (no `\'`).
  Compose Resources is not Android resources; both mistakes compile and only show on screen.
- Every database schema change: bump `version` in `data/AnchorDatabase.kt`, write a `Migration`
  and add it to `ANCHOR_MIGRATIONS` in `data/Migrations.kt`, and commit the exported JSON in
  `shared/schemas/`.
- New full-screen flows (no tab bar) are navigation destinations in `App.kt`
  (`modalComposable`), not overlay booleans. Close actions go through `dropUnlessResumed`.
- State that must survive rotation in a flow uses `rememberSaveable`.
- The two mood scales are opposite: mood **5 = Great**, crisis rating **1 = Great**. Convert
  only through `MoodScale.fromCrisisRating`.
- Tags go through `TagListConverter` / `ch.bfh.anchor.model.Tags`. Never join them by hand.

**Never**

- Use raw `Button`, `Surface(onClick)`, `TextField`, `Switch`, `ModalBottomSheet`, `TopAppBar`
  in screens. They look wrong on iOS.
- Hardcode user-visible text in Kotlin.
- Add `fallbackToDestructiveMigration`. This database is someone's therapy journal.
- Add network access, analytics, crash reporting or an account. The app is offline by design and
  the Android manifest has no `INTERNET` permission.
- Add a DI framework, a repository layer or `= viewModel()` defaults.
- Add a Material icons dependency (there is no multiplatform build; icons are vendored).
- Bump Kotlin, KSP, Compose Multiplatform, navigation or lifecycle, or add `iosX64`. See
  "Toolchain" below.
- Delete the `StageComposeResources` block in `androidApp/build.gradle.kts`. Without it every
  `stringResource()` throws at runtime on Android.
- Merge `shared` and `androidApp` back into one module. AGP 9 forbids it.
- Commit build output (`build/`) or local files (`local.properties`, `CLAUDE.md`).

## Copy and tone

This is a mental-health app, used on someone's worst day.

- Short sentences, no clinical jargon, no advice the app cannot stand behind.
- Never congratulate or cheer someone who has just said they still feel bad. Offer the next
  step (another round, a trusted person, the crisis line) instead.
- Crisis-flow copy is deliberate. Change it only when asked.

## Verify before you claim it works

Several bugs in this project compiled fine and only appeared at runtime, so a green compile is
not enough.

```bash
./gradlew :shared:iosSimulatorArm64Test             # unit tests (commonTest)
./gradlew :androidApp:assembleDebug                 # Android build
./gradlew :shared:compileKotlinIosSimulatorArm64    # fast iOS type-check
```

Then run it on both platforms:

```bash
# Android
adb shell am start -n ch.bfh.anchor/ch.bfh.anchor.MainActivity
adb exec-out screencap -p > /tmp/shot.png
adb logcat -d -s AndroidRuntime:E                   # crashes

# iOS
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 17' -derivedDataPath /tmp/dd build
xcrun simctl install "iPhone 17" /tmp/dd/Build/Products/Debug-iphonesimulator/Anchor.app
xcrun simctl launch --console-pty "iPhone 17" ch.bfh.anchor
xcrun simctl io "iPhone 17" screenshot /tmp/shot.png
```

For a schema change, also open the app on an existing database (not a fresh install) and check
the rows survived.

Say what you verified and what you could not, for example "iOS built and launched, not tapped".

## Toolchain

Pinned in `gradle/libs.versions.toml`, with the reason next to each pin:

- Kotlin **2.3.20** is both floor and ceiling: Compose Multiplatform 1.12.2 needs it, and KSP
  2.3.14 (for Room) caps Kotlin at 2.3.x.
- Navigation 2.9.2 and lifecycle 2.10.0 from **`org.jetbrains.androidx`**. The `androidx`
  artifacts have no iOS targets, and newer versions need Kotlin 2.4.
- `compileSdk` 37. No `iosX64`, so simulators need Apple Silicon.
- Room in KMP needs KSP per target (`kspAndroid`, `kspIosArm64`, `kspIosSimulatorArm64`) and
  the `expect object AnchorDatabaseConstructor`.

## Git

- Branch from `main`, one logical change per commit, each commit compiling on its own.
- A branch that is far behind `main` merges `main` into itself first and builds there. After a
  hand-resolved merge, check each file against both parents before pushing. Whole-file "ours" /
  "theirs" resolutions have silently dropped work here twice.
