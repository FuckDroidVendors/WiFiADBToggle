package fuck.wifiadbtoggle.droidvendorssuck

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

class MediaButtonService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_MEDIA_BUTTON_EVENT = "fuck.wifiadbtoggle.droidvendorssuck.action.MEDIA_BUTTON_EVENT"
        const val EXTRA_EVENT_NAME = "extra_event_name"
        const val EXTRA_EVENT_ACTION = "extra_event_action"

        fun start(context: Context) {
            // No-op in this flavor.
        }

        fun stop(context: Context) {
            // No-op in this flavor.
        }
    }
}
