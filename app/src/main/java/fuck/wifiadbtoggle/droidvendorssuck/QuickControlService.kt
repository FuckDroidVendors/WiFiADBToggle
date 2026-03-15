package fuck.wifiadbtoggle.droidvendorssuck

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class QuickControlService : Service() {
    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.isPersistentNotificationEnabled(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_desc)
            }
            manager.createNotificationChannel(channel)
        }

        val toggleIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, ShortcutActivity::class.java).setAction(ShortcutActivity.ACTION_TOGGLE),
            pendingFlags()
        )
        val enableIntent = PendingIntent.getActivity(
            this,
            2,
            Intent(this, ShortcutActivity::class.java).setAction(ShortcutActivity.ACTION_ENABLE),
            pendingFlags()
        )
        val disableIntent = PendingIntent.getActivity(
            this,
            3,
            Intent(this, ShortcutActivity::class.java).setAction(ShortcutActivity.ACTION_DISABLE),
            pendingFlags()
        )
        val settingsIntent = PendingIntent.getActivity(
            this,
            4,
            Intent(this, ShortcutActivity::class.java).setAction(ShortcutActivity.ACTION_SETTINGS),
            pendingFlags()
        )

        return NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_quick_title))
            .setContentText(getString(R.string.notif_quick_text))
            .setSmallIcon(R.drawable.ic_tile)
            .setOngoing(true)
            .addAction(0, getString(R.string.action_toggle), toggleIntent)
            .addAction(0, getString(R.string.action_enable), enableIntent)
            .addAction(0, getString(R.string.action_disable), disableIntent)
            .addAction(0, getString(R.string.action_settings), settingsIntent)
            .build()
    }

    private fun pendingFlags(): Int {
        return if (Build.VERSION.SDK_INT >= 23) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    }

    companion object {
        private const val NOTIF_CHANNEL_ID = "adb_quick_controls"
        private const val NOTIF_ID = 3001

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
        }
    }
}
