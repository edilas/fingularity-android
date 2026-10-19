package com.agosllc.fingularity

import android.app.Application
import com.google.android.gms.games.PlayGamesSdk

class FingularityApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val projectId = getString(R.string.game_services_project_id)
        if (projectId.isNotBlank() && projectId != "0000000000") {
            PlayGamesSdk.initialize(this)
        }
    }
}
