package fuck.wifiadbtoggle.droidvendorssuck;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

public final class NotificationHelper {
    private NotificationHelper() {
    }

    public static final int STATUS_NOTIF_ID = 1001;
    private static final String NOTIF_CHANNEL_ID = "adb_status";

    public static Notification buildStatusNotification(Context context) {
        boolean adbEnabled = AdbWifiController.isEnabled(context);
        int port = Settings.getAdbPort(context);
        NetworkUtils.IpResult ip = NetworkUtils.getActiveIp(context);
        String stateLabel = adbEnabled
            ? context.getString(R.string.value_on)
            : context.getString(R.string.value_off);
        String ipText = ip != null
            ? context.getString(R.string.ip_with_port, NetworkUtils.formatHostForPort(ip), port)
            : context.getString(R.string.tile_subtitle_no_ip);
        return buildStatusNotification(context, adbEnabled, ipText, stateLabel);
    }

    public static Notification buildStatusNotification(
        Context context,
        boolean adbEnabled,
        String ipText,
        String stateLabel
    ) {
        Notification.Builder builder = notificationBuilder(context);
        String actionLabel = context.getString(R.string.action_toggle);
        Intent toggleIntent = new Intent(context, NotifyActionReceiver.class)
            .setAction(NotifyActionReceiver.ACTION_TOGGLE);
        PendingIntent togglePending = PendingIntent.getBroadcast(
            context,
            1,
            toggleIntent,
            pendingFlags()
        );

        if (Build.VERSION.SDK_INT < 26) {
            return builder
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentTitle(context.getString(R.string.notif_status_title, stateLabel))
                .setContentText(context.getString(R.string.notif_status_text, ipText))
                .setContentIntent(togglePending)
                .addAction(0, actionLabel, togglePending)
                .build();
        }

        return builder
            .setContentTitle(context.getString(R.string.notif_status_title, stateLabel))
            .setContentText(context.getString(R.string.notif_status_text, ipText))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(togglePending)
            .addAction(0, actionLabel, togglePending)
            .build();
    }

    public static void notifyStatus(Context context) {
        if (!BuildConfig.FEATURE_NOTIFICATION) return;
        if (!canPostNotifications(context)) return;
        boolean adbEnabled = AdbWifiController.isEnabled(context);
        int port = Settings.getAdbPort(context);
        NetworkUtils.IpResult ip = NetworkUtils.getActiveIp(context);
        String stateLabel = adbEnabled
            ? context.getString(R.string.value_on)
            : context.getString(R.string.value_off);
        String ipText = ip != null
            ? context.getString(R.string.ip_with_port, NetworkUtils.formatHostForPort(ip), port)
            : context.getString(R.string.tile_subtitle_no_ip);

        String lastState = Settings.getLastNotifState(context);
        String lastIp = Settings.getLastNotifIp(context);
        if (lastState != null && lastIp != null && lastState.equals(stateLabel) && lastIp.equals(ipText)) {
            return;
        }
        Settings.setLastNotifState(context, stateLabel);
        Settings.setLastNotifIp(context, ipText);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(STATUS_NOTIF_ID, buildStatusNotification(context, adbEnabled, ipText, stateLabel));
    }

    public static void cancelStatus(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(STATUS_NOTIF_ID);
        Settings.clearLastNotif(context);
    }

    private static Notification.Builder notificationBuilder(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                NOTIF_CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(context.getString(R.string.notif_channel_desc));
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
            return new Notification.Builder(context, NOTIF_CHANNEL_ID);
        }
        return new Notification.Builder(context);
    }

    private static int pendingFlags() {
        return Build.VERSION.SDK_INT >= 23
            ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            : PendingIntent.FLAG_UPDATE_CURRENT;
    }

    private static boolean canPostNotifications(Context context) {
        if (Build.VERSION.SDK_INT < 33) return true;
        return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED;
    }
}
