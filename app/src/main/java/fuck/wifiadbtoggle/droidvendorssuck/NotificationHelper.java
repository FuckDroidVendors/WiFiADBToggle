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
import android.widget.RemoteViews;
import java.util.Collections;
import java.util.List;

public final class NotificationHelper {
    private NotificationHelper() {
    }

    public static final int STATUS_NOTIF_ID = 1001;
    public static final int CONNECTION_NOTIF_ID = 1002;
    private static final String NOTIF_CHANNEL_ID = "adb_monitor";

    public static Notification buildMonitorNotification(Context context) {
        Notification.Builder builder = notificationBuilder(context);
        return builder
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText(context.getString(R.string.notif_text))
            .setSmallIcon(R.drawable.ic_tile)
            .setOngoing(true)
            .build();
    }

    public static Notification buildStatusNotification(Context context) {
        boolean adbEnabled = AdbWifiController.isEnabled(context);
        int port = Settings.getAdbPort(context);
        String ip = NetworkUtils.getActiveIp(context);
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
        String actionLabel = adbEnabled
            ? context.getString(R.string.action_turn_off)
            : context.getString(R.string.action_turn_on);
        Intent toggleIntent = new Intent(context, ShortcutActivity.class).setAction(
            adbEnabled ? ShortcutActivity.ACTION_DISABLE : ShortcutActivity.ACTION_ENABLE
        );
        PendingIntent togglePending = PendingIntent.getActivity(
            context,
            1,
            toggleIntent,
            pendingFlags()
        );
        PendingIntent openPending = PendingIntent.getActivity(
            context,
            2,
            new Intent(context, ShortcutActivity.class).setAction(ShortcutActivity.ACTION_SETTINGS),
            pendingFlags()
        );

        if (Build.VERSION.SDK_INT < 26) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_status);
            remoteViews.setTextViewText(R.id.notif_title, context.getString(R.string.notif_status_title, stateLabel));
            remoteViews.setTextViewText(R.id.notif_ip, context.getString(R.string.notif_status_text, ipText));
            remoteViews.setTextViewText(R.id.notif_action, actionLabel);
            remoteViews.setOnClickPendingIntent(R.id.notif_action, togglePending);
            remoteViews.setOnClickPendingIntent(R.id.notif_root, togglePending);
            return builder
                .setSmallIcon(R.drawable.ic_tile)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(togglePending)
                .setContent(remoteViews)
                .build();
        }

        return builder
            .setContentTitle(context.getString(R.string.notif_status_title, stateLabel))
            .setContentText(context.getString(R.string.notif_status_text, ipText))
            .setSmallIcon(R.drawable.ic_tile)
            .setOngoing(true)
            .setContentIntent(togglePending)
            .addAction(0, actionLabel, togglePending)
            .addAction(0, context.getString(R.string.action_open), openPending)
            .build();
    }

    public static void notifyStatus(Context context) {
        if (!BuildConfig.FEATURE_NOTIFICATION) return;
        if (!canPostNotifications(context)) return;
        boolean adbEnabled = AdbWifiController.isEnabled(context);
        int port = Settings.getAdbPort(context);
        String ip = NetworkUtils.getActiveIp(context);
        String stateLabel = adbEnabled
            ? context.getString(R.string.value_on)
            : context.getString(R.string.value_off);
        String ipText = ip != null
            ? context.getString(R.string.ip_with_port, NetworkUtils.formatHostForPort(ip), port)
            : context.getString(R.string.tile_subtitle_no_ip);

        String lastState = Settings.getLastNotifState(context);
        String lastIp = Settings.getLastNotifIp(context);
        if (lastState != null && lastIp != null && lastState.equals(stateLabel) && lastIp.equals(ipText)) {
            notifyConnections(context);
            return;
        }
        Settings.setLastNotifState(context, stateLabel);
        Settings.setLastNotifIp(context, ipText);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(STATUS_NOTIF_ID, buildStatusNotification(context, adbEnabled, ipText, stateLabel));
        notifyConnections(context);
    }

    public static void cancelStatus(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(STATUS_NOTIF_ID);
        Settings.clearLastNotif(context);
    }

    public static void notifyConnections(Context context) {
        if (!BuildConfig.FEATURE_CONNECTIONS || !BuildConfig.FEATURE_NOTIFICATION) return;
        if (!Settings.isConnectionNotificationEnabled(context)) {
            cancelConnections(context);
            return;
        }
        if (!canPostNotifications(context)) return;
        ConnectionSummary summary = buildConnectionSummary(context);
        String last = Settings.getLastConnSummary(context);
        if (last != null && last.equals(summary.key)) return;
        Settings.setLastConnSummary(context, summary.key);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(CONNECTION_NOTIF_ID, summary.notification);
    }

    public static void cancelConnections(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(CONNECTION_NOTIF_ID);
        Settings.clearLastConnSummary(context);
    }

    private static final class ConnectionSummary {
        final String key;
        final Notification notification;

        ConnectionSummary(String key, Notification notification) {
            this.key = key;
            this.notification = notification;
        }
    }

    private static ConnectionSummary buildConnectionSummary(Context context) {
        Notification.Builder builder = notificationBuilder(context);
        PendingIntent openPending = PendingIntent.getActivity(
            context,
            3,
            new Intent(context, ShortcutActivity.class).setAction(ShortcutActivity.ACTION_SETTINGS),
            pendingFlags()
        );
        boolean hasRoot = ShellRunner.canUseRoot();
        if (!hasRoot) {
            Notification notification = builder
                .setContentTitle(context.getString(R.string.notif_conn_title))
                .setContentText(context.getString(R.string.notif_conn_root_required))
                .setSmallIcon(R.drawable.ic_tile)
                .setOngoing(true)
                .setContentIntent(openPending)
                .build();
            return new ConnectionSummary("root_required", notification);
        }

        int port = Settings.getAdbPort(context);
        AdbConnectionUtils.ConnectionInfo info = AdbConnectionUtils.getActiveConnections(context, port);
        List<String> hosts = info != null ? info.hosts : Collections.emptyList();
        int limit = Math.min(6, hosts.size());
        List<String> limited = hosts.subList(0, limit);
        int more = hosts.size() - limited.size();
        String countText = hosts.isEmpty()
            ? context.getString(R.string.notif_conn_none)
            : context.getString(R.string.notif_conn_count, hosts.size());
        String details;
        if (hosts.isEmpty()) {
            details = context.getString(R.string.notif_conn_none);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < limited.size(); i++) {
                if (i > 0) sb.append('\n');
                sb.append(limited.get(i));
            }
            if (more > 0) {
                sb.append("\n+");
                sb.append(more);
                sb.append(" ");
                sb.append(context.getString(R.string.notif_conn_more));
            }
            details = sb.toString();
        }
        Notification notification = builder
            .setContentTitle(context.getString(R.string.notif_conn_title))
            .setContentText(countText)
            .setStyle(new Notification.BigTextStyle().bigText(details))
            .setSmallIcon(R.drawable.ic_tile)
            .setOngoing(true)
            .setContentIntent(openPending)
            .setOnlyAlertOnce(true)
            .build();
        String key = port + "|" + hosts.size() + "|" + String.join(",", limited) + "|" + more;
        return new ConnectionSummary(key, notification);
    }

    private static Notification.Builder notificationBuilder(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            ensureChannel(context);
            return new Notification.Builder(context, NOTIF_CHANNEL_ID);
        }
        return new Notification.Builder(context);
    }

    private static int pendingFlags() {
        if (Build.VERSION.SDK_INT >= 23) {
            return PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.FLAG_UPDATE_CURRENT;
    }

    private static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(
                NOTIF_CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(context.getString(R.string.notif_channel_desc));
            manager.createNotificationChannel(channel);
        }
    }

    private static boolean canPostNotifications(Context context) {
        if (Build.VERSION.SDK_INT >= 33) {
            return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
}
