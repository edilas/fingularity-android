package com.agosllc.fingularity

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.JavascriptInterface

class GameBridge(
    private val activity: MainActivity,
    private val billing: BillingManager,
    private val playGames: PlayGamesManager
) {
    @JavascriptInterface
    fun haptic(kind: String) {
        activity.runOnUiThread { vibrate(kind) }
    }

    @JavascriptInterface
    fun purchase(@Suppress("UNUSED_PARAMETER") productId: String) {
        activity.runOnUiThread { billing.purchase() }
    }

    @JavascriptInterface
    fun restore(@Suppress("UNUSED_PARAMETER") productId: String) {
        activity.runOnUiThread { billing.restore() }
    }

    @JavascriptInterface
    fun submitScore(score: Double) {
        activity.runOnUiThread { playGames.submitScore(score.toLong()) }
    }

    @JavascriptInterface
    fun showLeaderboard() {
        activity.runOnUiThread { playGames.showLeaderboard() }
    }

    @JavascriptInterface
    fun log(message: String) {
        android.util.Log.d("FingularityJS", message)
    }

    private fun vibrate(kind: String) {
        val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activity.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            activity.getSystemService(Vibrator::class.java)
        }
        if (!vibrator.hasVibrator()) return

        val effect = when (kind) {
            "levelComplete" -> VibrationEffect.createWaveform(longArrayOf(0, 45, 45, 70), -1)
            "gameOver" -> VibrationEffect.createWaveform(longArrayOf(0, 90, 55, 120), -1)
            "destroy" -> VibrationEffect.createOneShot(42, 150)
            "voyagerPush" -> VibrationEffect.createOneShot(50, 120)
            "comet" -> VibrationEffect.createOneShot(35, 105)
            else -> VibrationEffect.createOneShot(18, 70)
        }
        vibrator.vibrate(effect)
    }
}
