package fuck.wifiadbtoggle.droidvendorssuck;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkMonitorService extends Service {

    private ConnectivityManager connectivityManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private PowerManager.WakeLock wakeLock = null;
    private boolean legacyReceiverRegistered = false;

    private final BroadcastReceiver legacyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scheduleEvaluate();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        startForeground(NotificationHelper.STATUS_NOTIF_ID, buildNotification());
        registerCallback();
        updateWakeLock();
        scheduleEvaluate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!Settings.isAutoStartEnabled(this) || !Settings.isAnyMonitorRuleEnabled(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(NotificationHelper.STATUS_NOTIF_ID, buildNotification());
        updateWakeLock();
        scheduleEvaluate();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterCallback();
        releaseWakeLock();
        executor.shutdown();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void registerCallback() {
        try {
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            registerReceiver(legacyReceiver, filter);
            legacyReceiverRegistered = true;
        } catch (Exception ignored) {
            // Ignore registration failures
        }
    }

    private void unregisterCallback() {
        try {
            if (legacyReceiverRegistered) {
                unregisterReceiver(legacyReceiver);
                legacyReceiverRegistered = false;
            }
        } catch (Exception ignored) {
            // Ignore unregister failures
        }
    }

    private void scheduleEvaluate() {
        executor.execute(this::evaluateAndApply);
    }

    private void evaluateAndApply() {
        try {
            if (!Settings.isAutoStartEnabled(this) || !Settings.isAnyMonitorRuleEnabled(this)) {
                stopSelf();
                return;
            }
            if (!ShellRunner.canUseRoot()) return;
            ScheduleMode scheduleMode = ScheduleManager.getActiveMode(this);
            if (scheduleMode == ScheduleMode.FORCE_ON) {
                if (!AdbWifiController.isEnabled(this)) {
                    AdbWifiController.enable(this);
                }
                return;
            }
            if (scheduleMode == ScheduleMode.FORCE_OFF) {
                if (AdbWifiController.isEnabled(this)) {
                    AdbWifiController.disable(this);
                }
                return;
            }

            NetworkInfo info = connectivityManager.getActiveNetworkInfo();
            boolean isConnected = info != null && info.isConnected();
            boolean onWifi = isConnected && info.getType() == ConnectivityManager.TYPE_WIFI;
            boolean onEthernet = isConnected && info.getType() == ConnectivityManager.TYPE_ETHERNET;

            if (!onWifi && !onEthernet) {
                if (Settings.isDisableOnDisconnectEnabled(this)) {
                    if (AdbWifiController.isEnabled(this)) {
                        AdbWifiController.disable(this);
                    }
                }
                return;
            }

            if (onEthernet && Settings.isAutoEnableEthernetEnabled(this)) {
                if (!AdbWifiController.isEnabled(this)) {
                    AdbWifiController.enable(this);
                }
                return;
            }

            if (onWifi && Settings.isAutoEnableSsidEnabled(this)) {
                WifiInfoResult wifiInfo = getWifiInfo();
                String ssid = wifiInfo != null ? wifiInfo.ssid : null;
                String bssid = wifiInfo != null ? wifiInfo.bssid : null;
                if (ssid == null) return;
                Set<String> allowedSsids = Settings.getSsidSet(this);
                if (!allowedSsids.isEmpty() && !allowedSsids.contains(Settings.normalizeSsid(ssid))) return;
                if (Settings.isFilterBssidEnabled(this)) {
                    if (bssid == null) return;
                    Set<String> allowedBssids = Settings.getBssidSet(this);
                    if (!allowedBssids.isEmpty() && !allowedBssids.contains(Settings.normalizeBssid(bssid))) return;
                }
                if (!AdbWifiController.isEnabled(this)) {
                    AdbWifiController.enable(this);
                }
            }
        } finally {
            updateStatusNotification();
        }
    }

    private WifiInfoResult getWifiInfo() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null || wifiManager.getConnectionInfo() == null) return null;
            String ssid = wifiManager.getConnectionInfo().getSSID();
            if (ssid == null || "<unknown ssid>".equals(ssid)) return null;
            String bssid = wifiManager.getConnectionInfo().getBSSID();
            return new WifiInfoResult(ssid, bssid);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void updateWakeLock() {
        if (Settings.isKeepAwakeEnabled(this)) {
            if (wakeLock == null || !wakeLock.isHeld()) {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getPackageName() + ":adb_monitor");
                wakeLock.acquire();
            }
        } else {
            releaseWakeLock();
        }
    }

    private void releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Exception ignored) {
            // Ignore
        } finally {
            wakeLock = null;
        }
    }

    private Notification buildNotification() {
        if (Settings.isPersistentNotificationEnabled(this)) {
            return NotificationHelper.buildStatusNotification(this);
        }
        return NotificationHelper.buildMonitorNotification(this);
    }

    public static void start(Context context) {
        if (!BuildConfig.FEATURE_MONITOR) return;
        if (!Settings.isAutoStartEnabled(context) || !Settings.isAnyMonitorRuleEnabled(context)) {
            return;
        }
        Intent intent = new Intent(context, NetworkMonitorService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, NetworkMonitorService.class);
        context.stopService(intent);
    }

    private void updateStatusNotification() {
        if (!Settings.isPersistentNotificationEnabled(this)) return;
        NotificationHelper.notifyStatus(this);
    }

    private static final class WifiInfoResult {
        final String ssid;
        final String bssid;

        WifiInfoResult(String ssid, String bssid) {
            this.ssid = ssid;
            this.bssid = bssid;
        }
    }
}
