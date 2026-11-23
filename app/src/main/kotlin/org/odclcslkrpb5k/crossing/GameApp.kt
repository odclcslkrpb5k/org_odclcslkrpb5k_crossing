package org.odclcslkrpb5k.crossing

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.StrictMode

/**
 * In debug builds, disk or network work on the main thread ends the game at once (frame budget).
 */
class GameApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .penaltyDeath()
                    .build()
            )
        }
    }
}
