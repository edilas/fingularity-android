# Google Play Console setup — Fingularity

## 1. Create the app

- App name: **Fingularity**
- Package name: **com.agosllc.fingularity**
- App/game: **Game**
- Pricing: choose your intended base app price/free status. The in-game level 11+ unlock is separate.

## 2. One-time product

Go to **Monetize with Play → Products → One-time products** and create:

- Product ID: `com.agosllc.fingularity.endless`
- Name: `The Endless Collapse`
- Description: `Unlock level 11 onward — the Celestial War, Fleet War, campaign bosses, the Null Sovereign ending, and endless play.`
- Purchase type: **Buy** (non-consumable)
- Suggested base price: **$4.99 USD** (or your current launch price)

Create/activate a purchase option and set regional availability/pricing. The Android app reads the localized Google Play price dynamically; do not hardcode a price in `game.html`.

## 3. Play Games Services v2 leaderboard

In **Grow users → Play Games Services** (location can vary as Play Console changes):

1. Create/associate a Play Games Services project for this app.
2. Copy the numeric **Game services project ID** into:
   `app/src/main/res/values/strings.xml` → `game_services_project_id`.
3. Create a leaderboard for **High Score**, larger is better.
4. Copy the generated Android leaderboard ID into:
   `app/src/main/res/values/strings.xml` → `leaderboard_id`.
5. Add your tester Google accounts and publish the Play Games Services configuration when ready.

The project uses Play Games Services **v2**, not deprecated v1.

## 4. Internal testing first

Create an **Internal testing** release, upload the signed `.aab`, add your tester Gmail accounts, and install from the Play testing link. This is the correct environment for end-to-end Billing + Play Games testing.

Test at minimum:

- New Game through level 10.
- Unlock card shows localized price.
- Successful test purchase unlocks level 11 immediately.
- Reinstall: entitlement is restored on launch.
- Restore Purchases works when already owned.
- Pending test purchase does **not** unlock until completed.
- Leaderboard button opens Play Games UI.
- Score submission reaches the Play Games leaderboard.
- Two-finger gameplay, haptics, audio, pause/back gesture, and final boss.

## 5. Build the Play upload bundle

In Android Studio:

**Build → Generate Signed App Bundle or APK → Android App Bundle**

Use your upload key, select `release`, and upload the generated `.aab` to the testing/release track.

## 6. Versioning

The initial Android port starts at:

- `versionCode = 1`
- `versionName = "1.0.0"`

Increment `versionCode` for every Play Console upload after the first one.

## 7. Privacy / Data Safety

An Android-specific privacy-policy draft is included at `privacy/privacy-policy-android.html`. Host it on your website and use its public HTTPS URL in Play Console.

The app itself has no Agos LLC account system, ads, or developer analytics. It does use Google Play Billing and, once configured, Google Play Games Services. Complete Play Console's Data Safety form based on the final SDK configuration and Google's current disclosure guidance rather than simply copying the old iOS “Data Not Collected” answer.
