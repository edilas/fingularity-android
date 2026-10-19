# Fingularity — Android / Google Play port

This project is the Android counterpart of the supplied Xcode build. The game itself is the same local HTML5/canvas game; the Swift/WKWebView shell has been replaced with a Kotlin/Android WebView shell.

## What is already ported

- Full game HTML, fonts, final-boss art, procedural audio, local progress and tutorial state.
- Full-screen portrait Android WebView with cutout-safe HUD support.
- Android haptics for swipe, destroy, Voyager push, level complete and game over.
- Google Play Billing one-time unlock for levels 11+.
- Automatic entitlement check on app launch/resume and a Restore Purchases path.
- Google Play Games Services v2 leaderboard bridge.
- Android 16 / target API 36 configuration for current Google Play submission requirements.
- App package: `com.agosllc.fingularity`.
- One-time product ID: `com.agosllc.fingularity.endless`.

## Open it

Open this folder in Android Studio and let Gradle sync. Use a physical Android device for touch, haptics, Play Billing and Play Games tests.

## Before Play Store testing

Two Play Console values are intentionally left as placeholders because Google generates them for your Play Games project:

1. `app/src/main/res/values/strings.xml` → `game_services_project_id`
2. `app/src/main/res/values/strings.xml` → `leaderboard_id`

See `PLAY_CONSOLE_SETUP.md` for the exact setup sequence.

## Billing test requirement

The purchase UI can load in a sideloaded build, but real/test Google Play Billing product details normally require the app to be installed from a Play test track with the product configured and the tester account enabled. Until then, the game remains fully playable through level 10 and the unlock reports a useful setup error.
