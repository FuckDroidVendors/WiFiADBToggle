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
        if (BuildConfig.FEATURE_MONITOR &&
            Settings.isAutoStartEnabled(context) &&
            Settings.isAnyMonitorRuleEnabled(context)
        ) {
            NetworkMonitorService.start(context);
        }
        if (BuildConfig.FEATURE_MEDIA && Settings.isMediaButtonsEnabled(context)) {
            MediaButtonService.start(context);
        }
        if (BuildConfig.FEATURE_NOTIFICATION && Settings.isPersistentNotificationEnabled(context)) {
            if (!(BuildConfig.FEATURE_MONITOR &&
                Settings.isAutoStartEnabled(context) &&
                Settings.isAnyMonitorRuleEnabled(context))
            ) {
                QuickControlService.start(context);
            }
        }
        if (BuildConfig.FEATURE_SCHEDULE && Settings.isScheduleEnabled(context)) {
            ScheduleAlarmScheduler.scheduleNext(context);
        }
    }
}
