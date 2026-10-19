# Port notes

The iOS build was already architected as a local HTML5 game hosted in WKWebView. This Android version therefore preserves the game instead of translating ~hundreds of KB of JavaScript gameplay code into another engine.

Native mappings:

| iOS | Android |
|---|---|
| WKWebView | android.webkit.WebView |
| StoreKit 2 | Google Play Billing 9.1.0 |
| Game Center | Google Play Games Services v2 |
| UIImpactFeedbackGenerator | VibrationEffect |
| UIKit safeAreaInsets | DisplayCutout safe insets |
| Bundle `web/` resources | `app/src/main/assets/` |

The local HTML bridge names are intentionally unchanged (`nativeIAP`, `nativeGameCenter`, `nativeHaptic`) so the core game does not need a gameplay rewrite.
