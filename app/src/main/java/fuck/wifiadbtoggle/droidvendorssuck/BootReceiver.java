package fuck.wifiadbtoggle.droidvendorssuck;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        if (BuildConfig.AUTOBOOT_ENABLE_ADB && !BuildConfig.SETTINGS_UI) {
            if (ShellRunner.canUseRoot()) {
                AdbWifiController.enableSilently(context);
            }
        }
        if (BuildConfig.FEATURE_NOTIFICATION && BuildConfig.FORCE_PERSISTENT_NOTIFICATION) {
            NotificationHelper.notifyStatus(context);
            return;
        }
        if (BuildConfig.FEATURE_MONITOR &&
            Settings.isAutoStartEnabled(context) &&
            Settings.isAnyMonitorRuleEnabled(context)
        ) {
            callStaticWithContext("fuck.wifiadbtoggle.droidvendorssuck.NetworkMonitorService", "start", context);
        }
        if (BuildConfig.FEATURE_MEDIA && Settings.isMediaButtonsEnabled(context)) {
            callStaticWithContext("fuck.wifiadbtoggle.droidvendorssuck.MediaButtonService", "start", context);
        }
        if (BuildConfig.FEATURE_NOTIFICATION && Settings.isPersistentNotificationEnabled(context)) {
            if (!(BuildConfig.FEATURE_MONITOR &&
                Settings.isAutoStartEnabled(context) &&
                Settings.isAnyMonitorRuleEnabled(context))
            ) {
                callStaticWithContext("fuck.wifiadbtoggle.droidvendorssuck.QuickControlService", "start", context);
            }
        }
        if (BuildConfig.FEATURE_SCHEDULE && Settings.isScheduleEnabled(context)) {
            callStaticWithContext("fuck.wifiadbtoggle.droidvendorssuck.ScheduleAlarmScheduler", "scheduleNext", context);
        }
    }

    private static void callStaticWithContext(String className, String methodName, Context context) {
        try {
            Class<?> clazz = Class.forName(className);
            java.lang.reflect.Method method = clazz.getMethod(methodName, Context.class);
            method.invoke(null, context);
        } catch (Exception ignored) {
        }
    }
}
