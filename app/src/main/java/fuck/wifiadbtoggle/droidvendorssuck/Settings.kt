package fuck.wifiadbtoggle.droidvendorssuck

import android.content.Context

object Settings {
    private const val PREFS = "wifi_adb_settings"
    private const val KEY_AUTO_START = "auto_start"
    private const val KEY_KEEP_AWAKE = "keep_awake"
    private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
    private const val KEY_AUTO_ENABLE_SSID = "auto_enable_ssid"
    private const val KEY_AUTO_ENABLE_ETHERNET = "auto_enable_ethernet"
    private const val KEY_DISABLE_ON_DISCONNECT = "disable_on_disconnect"
    private const val KEY_FILTER_BSSID = "filter_bssid"
    private const val KEY_SSID_LIST = "ssid_list"
    private const val KEY_BSSID_LIST = "bssid_list"
    private const val KEY_MEDIA_BUTTONS = "media_buttons"
    private const val KEY_MEDIA_PATTERN = "media_pattern"
    private const val KEY_PERSISTENT_NOTIFICATION = "persistent_notification"
    private const val KEY_CONN_NOTIFICATION = "adb_connection_notification"
    private const val KEY_ADB_PORT = "adb_port"
    private const val KEY_SCHEDULE_ENABLED = "schedule_enabled"
    private const val KEY_LAST_NOTIF_STATE = "last_notif_state"
    private const val KEY_LAST_NOTIF_IP = "last_notif_ip"
    private const val KEY_LAST_CONN_SUMMARY = "last_conn_summary"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isAutoStartEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_START, false)

    fun setAutoStartEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_START, enabled).apply()
    }

    fun isKeepAwakeEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_KEEP_AWAKE, false)

    fun setKeepAwakeEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_KEEP_AWAKE, enabled).apply()
    }

    fun isKeepScreenOnEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_KEEP_SCREEN_ON, false)

    fun setKeepScreenOnEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_KEEP_SCREEN_ON, enabled).apply()
    }

    fun isAutoEnableSsidEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_ENABLE_SSID, false)

    fun setAutoEnableSsidEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_ENABLE_SSID, enabled).apply()
    }

    fun isAutoEnableEthernetEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_ENABLE_ETHERNET, false)

    fun setAutoEnableEthernetEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_ENABLE_ETHERNET, enabled).apply()
    }

    fun isDisableOnDisconnectEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DISABLE_ON_DISCONNECT, false)

    fun setDisableOnDisconnectEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DISABLE_ON_DISCONNECT, enabled).apply()
    }

    fun isAnyMonitorRuleEnabled(context: Context): Boolean {
        return isAutoEnableSsidEnabled(context) ||
            isAutoEnableEthernetEnabled(context) ||
            isDisableOnDisconnectEnabled(context)
    }

    fun isFilterBssidEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FILTER_BSSID, false)

    fun setFilterBssidEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_FILTER_BSSID, enabled).apply()
    }

    fun getSsidList(context: Context): String =
        prefs(context).getString(KEY_SSID_LIST, "") ?: ""

    fun setSsidList(context: Context, value: String) {
        prefs(context).edit().putString(KEY_SSID_LIST, value).apply()
    }

    fun getBssidList(context: Context): String =
        prefs(context).getString(KEY_BSSID_LIST, "") ?: ""

    fun setBssidList(context: Context, value: String) {
        prefs(context).edit().putString(KEY_BSSID_LIST, value).apply()
    }

    fun getSsidSet(context: Context): Set<String> =
        parseList(getSsidList(context)).map { normalizeSsid(it) }.filter { it.isNotEmpty() }.toSet()

    fun getBssidSet(context: Context): Set<String> =
        parseList(getBssidList(context)).map { normalizeBssid(it) }.filter { it.isNotEmpty() }.toSet()

    fun addSsid(context: Context, ssid: String) {
        val set = getSsidSet(context).toMutableSet()
        set.add(normalizeSsid(ssid))
        setSsidList(context, set.joinToString(", "))
    }

    fun addBssid(context: Context, bssid: String) {
        val set = getBssidSet(context).toMutableSet()
        set.add(normalizeBssid(bssid))
        setBssidList(context, set.joinToString(", "))
    }

    fun parseList(raw: String): List<String> {
        return raw.split(",", ";", "\n", "\t")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun normalizeSsid(value: String): String {
        val trimmed = value.trim()
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length >= 2) {
            return trimmed.substring(1, trimmed.length - 1)
        }
        return trimmed
    }

    fun normalizeBssid(value: String): String =
        value.trim().lowercase()

    fun isMediaButtonsEnabled(context: Context): Boolean =
        if (!BuildConfig.FEATURE_MEDIA) false
        else prefs(context).getBoolean(KEY_MEDIA_BUTTONS, false)

    fun setMediaButtonsEnabled(context: Context, enabled: Boolean) {
        if (!BuildConfig.FEATURE_MEDIA) return
        prefs(context).edit().putBoolean(KEY_MEDIA_BUTTONS, enabled).apply()
    }

    fun getMediaPattern(context: Context): MediaPattern =
        if (!BuildConfig.FEATURE_MEDIA) MediaPattern.DOUBLE
        else MediaPattern.fromId(
            prefs(context).getString(KEY_MEDIA_PATTERN, MediaPattern.DOUBLE.id) ?: MediaPattern.DOUBLE.id
        )

    fun setMediaPattern(context: Context, pattern: MediaPattern) {
        if (!BuildConfig.FEATURE_MEDIA) return
        prefs(context).edit().putString(KEY_MEDIA_PATTERN, pattern.id).apply()
    }

    fun isPersistentNotificationEnabled(context: Context): Boolean =
        if (!BuildConfig.FEATURE_NOTIFICATION) false
        else prefs(context).getBoolean(KEY_PERSISTENT_NOTIFICATION, false)

    fun setPersistentNotificationEnabled(context: Context, enabled: Boolean) {
        if (!BuildConfig.FEATURE_NOTIFICATION) return
        prefs(context).edit().putBoolean(KEY_PERSISTENT_NOTIFICATION, enabled).apply()
    }

    fun isConnectionNotificationEnabled(context: Context): Boolean =
        if (!BuildConfig.FEATURE_CONNECTIONS) false
        else prefs(context).getBoolean(KEY_CONN_NOTIFICATION, false)

    fun setConnectionNotificationEnabled(context: Context, enabled: Boolean) {
        if (!BuildConfig.FEATURE_CONNECTIONS) return
        prefs(context).edit().putBoolean(KEY_CONN_NOTIFICATION, enabled).apply()
    }

    fun getAdbPort(context: Context): Int =
        prefs(context).getInt(KEY_ADB_PORT, AdbWifiController.DEFAULT_PORT)

    fun setAdbPort(context: Context, port: Int) {
        prefs(context).edit().putInt(KEY_ADB_PORT, port).apply()
    }

    fun isScheduleEnabled(context: Context): Boolean =
        if (!BuildConfig.FEATURE_SCHEDULE) false
        else prefs(context).getBoolean(KEY_SCHEDULE_ENABLED, false)

    fun setScheduleEnabled(context: Context, enabled: Boolean) {
        if (!BuildConfig.FEATURE_SCHEDULE) return
        prefs(context).edit().putBoolean(KEY_SCHEDULE_ENABLED, enabled).apply()
    }

    fun getLastNotifState(context: Context): String? =
        prefs(context).getString(KEY_LAST_NOTIF_STATE, null)

    fun setLastNotifState(context: Context, value: String?) {
        prefs(context).edit().putString(KEY_LAST_NOTIF_STATE, value).apply()
    }

    fun getLastNotifIp(context: Context): String? =
        prefs(context).getString(KEY_LAST_NOTIF_IP, null)

    fun setLastNotifIp(context: Context, value: String?) {
        prefs(context).edit().putString(KEY_LAST_NOTIF_IP, value).apply()
    }

    fun clearLastNotif(context: Context) {
        prefs(context).edit()
            .remove(KEY_LAST_NOTIF_STATE)
            .remove(KEY_LAST_NOTIF_IP)
            .apply()
    }

    fun getLastConnSummary(context: Context): String? =
        prefs(context).getString(KEY_LAST_CONN_SUMMARY, null)

    fun setLastConnSummary(context: Context, value: String?) {
        prefs(context).edit().putString(KEY_LAST_CONN_SUMMARY, value).apply()
    }

    fun clearLastConnSummary(context: Context) {
        prefs(context).edit().remove(KEY_LAST_CONN_SUMMARY).apply()
    }
}

enum class MediaPattern(val id: String) {
    SINGLE("single"),
    DOUBLE("double"),
    TRIPLE("triple"),
    LONG("long");

    companion object {
        fun fromId(id: String): MediaPattern {
            return values().firstOrNull { it.id == id } ?: DOUBLE
        }
    }
}
