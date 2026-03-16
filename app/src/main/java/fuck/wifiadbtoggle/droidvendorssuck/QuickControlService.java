package fuck.wifiadbtoggle.droidvendorssuck;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class QuickControlService extends Service {
    private Handler connHandler = null;
    private Runnable connPollRunnable = null;

    @Override
    public void onCreate() {
        super.onCreate();
        if (!BuildConfig.FEATURE_NOTIFICATION) {
            stopSelf();
            return;
        }
        if (Build.VERSION.SDK_INT >= 26) {
            startForeground(NotificationHelper.STATUS_NOTIF_ID, buildNotification());
            NotificationHelper.notifyConnections(this);
        } else {
            NotificationHelper.notifyStatus(this);
            if (Settings.isConnectionNotificationEnabled(this)) {
                startConnectionPolling();
            } else {
                stopSelf();
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!BuildConfig.FEATURE_NOTIFICATION) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!Settings.isPersistentNotificationEnabled(this)) {
            NotificationHelper.cancelStatus(this);
            stopConnectionPolling();
            stopSelf();
            return START_NOT_STICKY;
        }
        if (Build.VERSION.SDK_INT < 26) {
            NotificationHelper.notifyStatus(this);
            if (Settings.isConnectionNotificationEnabled(this)) {
                startConnectionPolling();
                return START_STICKY;
            }
            stopSelf();
            return START_NOT_STICKY;
        }
        if (BuildConfig.FEATURE_CONNECTIONS && Settings.isConnectionNotificationEnabled(this)) {
            startConnectionPolling();
        } else {
            stopConnectionPolling();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopConnectionPolling();
        super.onDestroy();
    }

    private Notification buildNotification() {
        return NotificationHelper.buildStatusNotification(this);
    }

    private void startConnectionPolling() {
        ensureConnectionPolling();
        Handler handler = connHandler;
        Runnable runnable = connPollRunnable;
        if (handler == null || runnable == null) return;
        handler.removeCallbacks(runnable);
        handler.post(runnable);
    }

    private void stopConnectionPolling() {
        Handler handler = connHandler;
        Runnable runnable = connPollRunnable;
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
        connHandler = null;
        connPollRunnable = null;
    }

    private void ensureConnectionPolling() {
        if (connHandler != null && connPollRunnable != null) return;
        Handler handler = new Handler(Looper.getMainLooper());
        connHandler = handler;
        connPollRunnable = new Runnable() {
            @Override
            public void run() {
                NotificationHelper.notifyConnections(QuickControlService.this);
                handler.postDelayed(this, CONNECTION_POLL_MS);
            }
        };
    }

    private static final long CONNECTION_POLL_MS = 4000L;

    public static void start(Context context) {
        if (!BuildConfig.FEATURE_NOTIFICATION) return;
        if (!Settings.isPersistentNotificationEnabled(context)) return;
        Intent intent = new Intent(context, QuickControlService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, QuickControlService.class);
        context.stopService(intent);
        NotificationHelper.cancelStatus(context);
    }
}
