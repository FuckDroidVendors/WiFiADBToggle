package fuck.wifiadbtoggle.droidvendorssuck;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class Settings {
    private Settings() {
    }

    private static final String PREFS = "wifi_adb_settings";
    private static final String KEY_AUTO_START = "auto_start";
    private static final String KEY_KEEP_AWAKE = "keep_awake";
    private static final String KEY_KEEP_SCREEN_ON = "keep_screen_on";
    private static final String KEY_AUTO_ENABLE_SSID = "auto_enable_ssid";
    private static final String KEY_AUTO_ENABLE_ETHERNET = "auto_enable_ethernet";
    private static final String KEY_DISABLE_ON_DISCONNECT = "disable_on_disconnect";
    private static final String KEY_FILTER_BSSID = "filter_bssid";
    private static final String KEY_SSID_LIST = "ssid_list";
    private static final String KEY_BSSID_LIST = "bssid_list";
    private static final String KEY_MEDIA_BUTTONS = "media_buttons";
    private static final String KEY_MEDIA_PATTERN = "media_pattern";
    private static final String KEY_PERSISTENT_NOTIFICATION = "persistent_notification";
    private static final String KEY_CONN_NOTIFICATION = "adb_connection_notification";
    private static final String KEY_ADB_PORT = "adb_port";
    private static final String KEY_SCHEDULE_ENABLED = "schedule_enabled";
    private static final String KEY_LAST_NOTIF_STATE = "last_notif_state";
    private static final String KEY_LAST_NOTIF_IP = "last_notif_ip";
    private static final String KEY_LAST_CONN_SUMMARY = "last_conn_summary";

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isAutoStartEnabled(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_START, false);
    }

    public static void setAutoStartEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_AUTO_START, enabled).apply();
    }

    public static boolean isKeepAwakeEnabled(Context context) {
        return prefs(context).getBoolean(KEY_KEEP_AWAKE, false);
    }

    public static void setKeepAwakeEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_KEEP_AWAKE, enabled).apply();
    }

    public static boolean isKeepScreenOnEnabled(Context context) {
        return prefs(context).getBoolean(KEY_KEEP_SCREEN_ON, false);
    }

    public static void setKeepScreenOnEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_KEEP_SCREEN_ON, enabled).apply();
    }

    public static boolean isAutoEnableSsidEnabled(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_ENABLE_SSID, false);
    }

    public static void setAutoEnableSsidEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_AUTO_ENABLE_SSID, enabled).apply();
    }

    public static boolean isAutoEnableEthernetEnabled(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_ENABLE_ETHERNET, false);
    }

    public static void setAutoEnableEthernetEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_AUTO_ENABLE_ETHERNET, enabled).apply();
    }

    public static boolean isDisableOnDisconnectEnabled(Context context) {
        return prefs(context).getBoolean(KEY_DISABLE_ON_DISCONNECT, false);
    }

    public static void setDisableOnDisconnectEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_DISABLE_ON_DISCONNECT, enabled).apply();
    }

    public static boolean isAnyMonitorRuleEnabled(Context context) {
        return isAutoEnableSsidEnabled(context) ||
            isAutoEnableEthernetEnabled(context) ||
            isDisableOnDisconnectEnabled(context);
    }

    public static boolean isFilterBssidEnabled(Context context) {
        return prefs(context).getBoolean(KEY_FILTER_BSSID, false);
    }

    public static void setFilterBssidEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_FILTER_BSSID, enabled).apply();
    }

    public static String getSsidList(Context context) {
        String value = prefs(context).getString(KEY_SSID_LIST, "");
        return value != null ? value : "";
    }

    public static void setSsidList(Context context, String value) {
        prefs(context).edit().putString(KEY_SSID_LIST, value).apply();
    }

    public static String getBssidList(Context context) {
        String value = prefs(context).getString(KEY_BSSID_LIST, "");
        return value != null ? value : "";
    }

    public static void setBssidList(Context context, String value) {
        prefs(context).edit().putString(KEY_BSSID_LIST, value).apply();
    }

    public static Set<String> getSsidSet(Context context) {
        List<String> items = parseList(getSsidList(context));
        Set<String> set = new HashSet<>();
        for (String item : items) {
            String normalized = normalizeSsid(item);
            if (!normalized.isEmpty()) {
                set.add(normalized);
            }
        }
        return set;
    }

    public static Set<String> getBssidSet(Context context) {
        List<String> items = parseList(getBssidList(context));
        Set<String> set = new HashSet<>();
        for (String item : items) {
            String normalized = normalizeBssid(item);
            if (!normalized.isEmpty()) {
                set.add(normalized);
            }
        }
        return set;
    }

    public static void addSsid(Context context, String ssid) {
        Set<String> set = new HashSet<>(getSsidSet(context));
        set.add(normalizeSsid(ssid));
        setSsidList(context, String.join(", ", set));
    }

    public static void addBssid(Context context, String bssid) {
        Set<String> set = new HashSet<>(getBssidSet(context));
        set.add(normalizeBssid(bssid));
        setBssidList(context, String.join(", ", set));
    }

    public static List<String> parseList(String raw) {
        String[] parts = raw.split("[,;\\n\\t]");
        List<String> items = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                items.add(trimmed);
            }
        }
        return items;
    }

    public static String normalizeSsid(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    public static String normalizeBssid(String value) {
        return value.trim().toLowerCase(Locale.US);
    }

    public static boolean isMediaButtonsEnabled(Context context) {
        if (!BuildConfig.FEATURE_MEDIA) return false;
        return prefs(context).getBoolean(KEY_MEDIA_BUTTONS, false);
    }

    public static void setMediaButtonsEnabled(Context context, boolean enabled) {
        if (!BuildConfig.FEATURE_MEDIA) return;
        prefs(context).edit().putBoolean(KEY_MEDIA_BUTTONS, enabled).apply();
    }

    public static MediaPattern getMediaPattern(Context context) {
        if (!BuildConfig.FEATURE_MEDIA) return MediaPattern.DOUBLE;
        String value = prefs(context).getString(KEY_MEDIA_PATTERN, MediaPattern.DOUBLE.id);
        return MediaPattern.fromId(value != null ? value : MediaPattern.DOUBLE.id);
    }

    public static void setMediaPattern(Context context, MediaPattern pattern) {
        if (!BuildConfig.FEATURE_MEDIA) return;
        prefs(context).edit().putString(KEY_MEDIA_PATTERN, pattern.id).apply();
    }

    public static boolean isPersistentNotificationEnabled(Context context) {
        if (!BuildConfig.FEATURE_NOTIFICATION) return false;
        if (BuildConfig.FORCE_PERSISTENT_NOTIFICATION) return true;
        return prefs(context).getBoolean(KEY_PERSISTENT_NOTIFICATION, false);
    }

    public static void setPersistentNotificationEnabled(Context context, boolean enabled) {
        if (!BuildConfig.FEATURE_NOTIFICATION) return;
        if (BuildConfig.FORCE_PERSISTENT_NOTIFICATION) return;
        prefs(context).edit().putBoolean(KEY_PERSISTENT_NOTIFICATION, enabled).apply();
    }

    public static boolean isConnectionNotificationEnabled(Context context) {
        if (!BuildConfig.FEATURE_CONNECTIONS) return false;
        return prefs(context).getBoolean(KEY_CONN_NOTIFICATION, false);
    }

    public static void setConnectionNotificationEnabled(Context context, boolean enabled) {
        if (!BuildConfig.FEATURE_CONNECTIONS) return;
        prefs(context).edit().putBoolean(KEY_CONN_NOTIFICATION, enabled).apply();
    }

    public static int getAdbPort(Context context) {
        if (!BuildConfig.SETTINGS_UI) return BuildConfig.COMPILE_ADB_PORT;
        return prefs(context).getInt(KEY_ADB_PORT, BuildConfig.COMPILE_ADB_PORT);
    }

    public static void setAdbPort(Context context, int port) {
        if (!BuildConfig.SETTINGS_UI) return;
        prefs(context).edit().putInt(KEY_ADB_PORT, port).apply();
    }

    public static boolean isScheduleEnabled(Context context) {
        if (!BuildConfig.FEATURE_SCHEDULE) return false;
        return prefs(context).getBoolean(KEY_SCHEDULE_ENABLED, false);
    }

    public static void setScheduleEnabled(Context context, boolean enabled) {
        if (!BuildConfig.FEATURE_SCHEDULE) return;
        prefs(context).edit().putBoolean(KEY_SCHEDULE_ENABLED, enabled).apply();
    }

    public static String getLastNotifState(Context context) {
        return prefs(context).getString(KEY_LAST_NOTIF_STATE, null);
    }

    public static void setLastNotifState(Context context, String value) {
        prefs(context).edit().putString(KEY_LAST_NOTIF_STATE, value).apply();
    }

    public static String getLastNotifIp(Context context) {
        return prefs(context).getString(KEY_LAST_NOTIF_IP, null);
    }

    public static void setLastNotifIp(Context context, String value) {
        prefs(context).edit().putString(KEY_LAST_NOTIF_IP, value).apply();
    }

    public static void clearLastNotif(Context context) {
        prefs(context).edit()
            .remove(KEY_LAST_NOTIF_STATE)
            .remove(KEY_LAST_NOTIF_IP)
            .apply();
    }

    public static String getLastConnSummary(Context context) {
        return prefs(context).getString(KEY_LAST_CONN_SUMMARY, null);
    }

    public static void setLastConnSummary(Context context, String value) {
        prefs(context).edit().putString(KEY_LAST_CONN_SUMMARY, value).apply();
    }

    public static void clearLastConnSummary(Context context) {
        prefs(context).edit().remove(KEY_LAST_CONN_SUMMARY).apply();
    }
}
