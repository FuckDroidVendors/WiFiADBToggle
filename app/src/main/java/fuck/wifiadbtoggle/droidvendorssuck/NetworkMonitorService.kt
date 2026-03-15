package fuck.wifiadbtoggle.droidvendorssuck

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class NetworkMonitorService : Service() {

    private lateinit var connectivityManager: ConnectivityManager
    private val executor = Executors.newSingleThreadExecutor()
    private var wakeLock: PowerManager.WakeLock? = null
    private var legacyReceiverRegistered = false

    private val legacyReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            scheduleEvaluate()
        }
    }

    override fun onCreate() {
        super.onCreate()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        startForeground(NOTIF_ID, buildNotification())
        registerCallback()
        updateWakeLock()
        scheduleEvaluate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.isAutoStartEnabled(this) || !Settings.isAnyMonitorRuleEnabled(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        updateWakeLock()
        scheduleEvaluate()
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterCallback()
        releaseWakeLock()
        executor.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerCallback() {
        try {
            val filter = android.content.IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            registerReceiver(legacyReceiver, filter)
            legacyReceiverRegistered = true
        } catch (_: Exception) {
            // Ignore registration failures
        }
    }

    private fun unregisterCallback() {
        try {
            if (legacyReceiverRegistered) {
                unregisterReceiver(legacyReceiver)
                legacyReceiverRegistered = false
            }
        } catch (_: Exception) {
            // Ignore unregister failures
        }
    }

    private fun scheduleEvaluate() {
        executor.execute { evaluateAndApply() }
    }

    private fun evaluateAndApply() {
        if (!Settings.isAutoStartEnabled(this) || !Settings.isAnyMonitorRuleEnabled(this)) {
            stopSelf()
            return
        }
        if (!ShellRunner.canUseRoot()) return
        val scheduleMode = ScheduleManager.getActiveMode(this)
        if (scheduleMode == ScheduleMode.FORCE_ON) {
            if (!AdbWifiController.isEnabled(this)) {
                AdbWifiController.enable(this)
            }
            return
        }
        if (scheduleMode == ScheduleMode.FORCE_OFF) {
            if (AdbWifiController.isEnabled(this)) {
                AdbWifiController.disable(this)
            }
            return
        }

        val info = connectivityManager.activeNetworkInfo
        val isConnected = info?.isConnected == true
        val onWifi = isConnected && info?.type == ConnectivityManager.TYPE_WIFI
        val onEthernet = isConnected && info?.type == ConnectivityManager.TYPE_ETHERNET

        if (!onWifi && !onEthernet) {
            if (Settings.isDisableOnDisconnectEnabled(this)) {
                if (AdbWifiController.isEnabled(this)) {
                    AdbWifiController.disable(this)
                }
            }
            return
        }

        if (onEthernet && Settings.isAutoEnableEthernetEnabled(this)) {
            if (!AdbWifiController.isEnabled(this)) {
                AdbWifiController.enable(this)
            }
            return
        }

        if (onWifi && Settings.isAutoEnableSsidEnabled(this)) {
            val wifiInfo = getWifiInfo()
            val ssid = wifiInfo?.first
            val bssid = wifiInfo?.second
            if (ssid == null) return
            val allowedSsids = Settings.getSsidSet(this)
            if (allowedSsids.isNotEmpty() && !allowedSsids.contains(Settings.normalizeSsid(ssid))) return
            if (Settings.isFilterBssidEnabled(this)) {
                if (bssid == null) return
                val allowedBssids = Settings.getBssidSet(this)
                if (allowedBssids.isNotEmpty() && !allowedBssids.contains(Settings.normalizeBssid(bssid))) return
            }
            if (!AdbWifiController.isEnabled(this)) {
                AdbWifiController.enable(this)
            }
        }
    }

    private fun getWifiInfo(): Pair<String, String?>? {
        return try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wifiManager.connectionInfo ?: return null
            val ssid = info.ssid?.takeIf { it != "<unknown ssid>" } ?: return null
            ssid to info.bssid
        } catch (_: Exception) {
            null
        }
    }

    private fun updateWakeLock() {
        if (Settings.isKeepAwakeEnabled(this)) {
            if (wakeLock?.isHeld != true) {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:adb_monitor")
                wakeLock?.acquire()
            }
        } else {
            releaseWakeLock()
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.takeIf { it.isHeld }?.release()
        } catch (_: Exception) {
            // Ignore
        } finally {
            wakeLock = null
        }
    }

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
        return NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setSmallIcon(R.drawable.ic_tile)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIF_CHANNEL_ID = "adb_monitor"
        private const val NOTIF_ID = 1001

        fun start(context: Context) {
            if (!Settings.isAutoStartEnabled(context) || !Settings.isAnyMonitorRuleEnabled(context)) {
                return
            }
            val intent = Intent(context, NetworkMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, NetworkMonitorService::class.java)
            context.stopService(intent)
        }
    }
}
