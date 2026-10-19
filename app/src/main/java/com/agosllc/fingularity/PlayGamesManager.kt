package com.agosllc.fingularity

import android.app.Activity
import android.content.Intent
import com.google.android.gms.games.PlayGames

class PlayGamesManager(
    private val activity: Activity,
    private val onAvailability: (Boolean) -> Unit
) {
    private val configured: Boolean
        get() {
            val projectId = activity.getString(R.string.game_services_project_id)
            val leaderboardId = activity.getString(R.string.leaderboard_id)
            return projectId.isNotBlank() && projectId != "0000000000" &&
                leaderboardId.isNotBlank() && !leaderboardId.startsWith("REPLACE_")
        }

    fun refreshAvailability() {
        if (!configured) {
            onAvailability(false)
            return
        }
        // The button is useful even if automatic authentication has not completed;
        // showLeaderboard() can explicitly request sign-in when the player taps it.
        onAvailability(true)
    }

    fun submitScore(score: Long) {
        if (!configured || score <= 0) return
        val signIn = PlayGames.getGamesSignInClient(activity)
        signIn.isAuthenticated.addOnCompleteListener { task ->
            if (task.isSuccessful && task.result.isAuthenticated) {
                PlayGames.getLeaderboardsClient(activity)
                    .submitScore(activity.getString(R.string.leaderboard_id), score)
            }
        }
    }

    fun showLeaderboard() {
        if (!configured) return
        val signIn = PlayGames.getGamesSignInClient(activity)
        signIn.isAuthenticated.addOnCompleteListener { task ->
            val authenticated = task.isSuccessful && task.result.isAuthenticated
            if (authenticated) {
                openLeaderboard()
            } else {
                signIn.signIn().addOnCompleteListener { signInTask ->
                    if (signInTask.isSuccessful && signInTask.result.isAuthenticated) {
                        openLeaderboard()
                    }
                }
            }
        }
    }

    private fun openLeaderboard() {
        PlayGames.getLeaderboardsClient(activity)
            .getLeaderboardIntent(activity.getString(R.string.leaderboard_id))
            .addOnSuccessListener { intent: Intent ->
                @Suppress("DEPRECATION")
                activity.startActivityForResult(intent, RC_LEADERBOARD_UI)
            }
    }

    companion object {
        private const val RC_LEADERBOARD_UI = 9004
    }
}
