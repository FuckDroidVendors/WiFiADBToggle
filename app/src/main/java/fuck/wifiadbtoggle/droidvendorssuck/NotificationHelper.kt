package fuck.wifiadbtoggle.droidvendorssuck

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object NotificationHelper {
    const val STATUS_NOTIF_ID = 1001
    private const val NOTIF_CHANNEL_ID = "adb_monitor"

    fun buildMonitorNotification(context: Context): Notification {
        ensureChannel(context)
        return NotificationCompat.Builder(context, NOTIF_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText(context.getString(R.string.notif_text))
            .setSmallIcon(R.drawable.ic_tile)
            .setOngoing(true)
            .build()
    }

    fun buildStatusNotification(context: Context): Notification {
        val adbEnabled = AdbWifiController.isEnabled(context)
        val ip = NetworkUtils.getActiveIp(context)
        val stateLabel = if (adbEnabled) context.getString(R.string.value_on) else context.getString(R.string.value_off)
        val ipText = ip?.let { context.getString(R.string.ip_with_port, NetworkUtils.formatHostForPort(it)) }
            ?: context.getString(R.string.tile_subtitle_no_ip)
        return buildStatusNotification(context, adbEnabled, ipText, stateLabel)
    }

    fun buildStatusNotification(
        context: Context,
        adbEnabled: Boolean,
        ipText: String,
        stateLabel: String
    ): Notification {
        ensureChannel(context)
        val actionLabel = if (adbEnabled) {
            context.getString(R.string.action_turn_off)
        } else {
            context.getString(R.string.action_turn_on)
        }
        val toggleIntent = Intent(context, ShortcutActivity::class.java).setAction(
            if (adbEnabled) ShortcutActivity.ACTION_DISABLE else ShortcutActivity.ACTION_ENABLE
        )
        val togglePending = PendingIntent.getActivity(
            context,
            1,
            toggleIntent,
            pendingFlags()
        )
        val openPending = PendingIntent.getActivity(
            context,
            2,
            Intent(context, MainActivity::class.java),
            pendingFlags()
        )

        if (Build.VERSION.SDK_INT < 26) {
            val remoteViews = RemoteViews(context.packageName, R.layout.notification_status).apply {
                setTextViewText(R.id.notif_title, context.getString(R.string.notif_status_title, stateLabel))
                setTextViewText(R.id.notif_ip, context.getString(R.string.notif_status_text, ipText))
                setTextViewText(R.id.notif_action, actionLabel)
                setOnClickPendingIntent(R.id.notif_action, togglePending)
                setOnClickPendingIntent(R.id.notif_root, togglePending)
            }
            return NotificationCompat.Builder(context, NOTIF_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_tile)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(togglePending)
                .setCustomContentView(remoteViews)
                .build()
        }

        return NotificationCompat.Builder(context, NOTIF_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notif_status_title, stateLabel))
            .setContentText(context.getString(R.string.notif_status_text, ipText))
            .setSmallIcon(R.drawable.ic_tile)
            .setOngoing(true)
            .setContentIntent(togglePending)
            .addAction(0, actionLabel, togglePending)
            .addAction(0, context.getString(R.string.action_open), openPending)
            .build()
    }

    fun notifyStatus(context: Context) {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val adbEnabled = AdbWifiController.isEnabled(context)
        val ip = NetworkUtils.getActiveIp(context)
        val stateLabel = if (adbEnabled) context.getString(R.string.value_on) else context.getString(R.string.value_off)
        val ipText = ip?.let { context.getString(R.string.ip_with_port, NetworkUtils.formatHostForPort(it)) }
            ?: context.getString(R.string.tile_subtitle_no_ip)

        val lastState = Settings.getLastNotifState(context)
        val lastIp = Settings.getLastNotifIp(context)
        if (lastState != null && lastIp != null && lastState == stateLabel && lastIp == ipText) {
            return
        }
        Settings.setLastNotifState(context, stateLabel)
        Settings.setLastNotifIp(context, ipText)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(STATUS_NOTIF_ID, buildStatusNotification(context, adbEnabled, ipText, stateLabel))
    }

    fun cancelStatus(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(STATUS_NOTIF_ID)
        Settings.clearLastNotif(context)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_desc)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun pendingFlags(): Int {
        return if (Build.VERSION.SDK_INT >= 23) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    }
}
