package fuck.wifiadbtoggle.droidvendorssuck

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper

class QuickControlService : Service() {
    private val connHandler = Handler(Looper.getMainLooper())
    private val connPollRunnable = object : Runnable {
        override fun run() {
            NotificationHelper.notifyConnections(this@QuickControlService)
            connHandler.postDelayed(this, CONNECTION_POLL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (!BuildConfig.FEATURE_NOTIFICATION) {
            stopSelf()
            return
        }
        if (Build.VERSION.SDK_INT >= 26) {
            startForeground(NotificationHelper.STATUS_NOTIF_ID, buildNotification())
            NotificationHelper.notifyConnections(this)
        } else {
            NotificationHelper.notifyStatus(this)
            if (Settings.isConnectionNotificationEnabled(this)) {
                startConnectionPolling()
            } else {
                stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!BuildConfig.FEATURE_NOTIFICATION) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!Settings.isPersistentNotificationEnabled(this)) {
            NotificationHelper.cancelStatus(this)
            stopConnectionPolling()
            stopSelf()
            return START_NOT_STICKY
        }
        if (Build.VERSION.SDK_INT < 26) {
            NotificationHelper.notifyStatus(this)
            if (Settings.isConnectionNotificationEnabled(this)) {
                startConnectionPolling()
                return START_STICKY
            }
            stopSelf()
            return START_NOT_STICKY
        }
        if (BuildConfig.FEATURE_CONNECTIONS && Settings.isConnectionNotificationEnabled(this)) {
            startConnectionPolling()
        } else {
            stopConnectionPolling()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopConnectionPolling()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        return NotificationHelper.buildStatusNotification(this)
    }

    private fun startConnectionPolling() {
        connHandler.removeCallbacks(connPollRunnable)
        connHandler.post(connPollRunnable)
    }

    private fun stopConnectionPolling() {
        connHandler.removeCallbacks(connPollRunnable)
    }

    companion object {
        private const val CONNECTION_POLL_MS = 4000L

        fun start(context: Context) {
            if (!BuildConfig.FEATURE_NOTIFICATION) return
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
