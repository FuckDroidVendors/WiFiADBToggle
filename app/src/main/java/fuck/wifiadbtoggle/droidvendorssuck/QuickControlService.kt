package fuck.wifiadbtoggle.droidvendorssuck

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder

class QuickControlService : Service() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            startForeground(NotificationHelper.STATUS_NOTIF_ID, buildNotification())
            NotificationHelper.notifyConnections(this)
        } else {
            NotificationHelper.notifyStatus(this)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.isPersistentNotificationEnabled(this)) {
            NotificationHelper.cancelStatus(this)
            stopSelf()
            return START_NOT_STICKY
        }
        if (Build.VERSION.SDK_INT < 26) {
            NotificationHelper.notifyStatus(this)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        return NotificationHelper.buildStatusNotification(this)
    }

    companion object {
        fun start(context: Context) {
            if (!Settings.isPersistentNotificationEnabled(context)) return
            val intent = Intent(context, QuickControlService::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, QuickControlService::class.java)
            context.stopService(intent)
            NotificationHelper.cancelStatus(context)
        }
    }
}
