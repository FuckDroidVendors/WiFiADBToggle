package fuck.wifiadbtoggle.droidvendorssuck;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotifyBootReceiver extends BroadcastReceiver {
    private static final String TAG = "NotifyBoot";
    private static final boolean LOG_ENABLED = BuildConfig.AUTOBOOT_ENABLE_ADB;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        boolean lockedBoot = Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action);
        boolean normalBoot = Intent.ACTION_BOOT_COMPLETED.equals(action);
        if (!lockedBoot && !normalBoot) return;
        if (lockedBoot && !BuildConfig.LOCKED_BOOT_ENABLED) return;

        if (LOG_ENABLED) {
            Log.d(TAG, "onReceive action=" + action + " lockedBoot=" + lockedBoot + " normalBoot=" + normalBoot);
        }
        if (BuildConfig.AUTOBOOT_ENABLE_ADB && !BuildConfig.SETTINGS_UI) {
            if (ShellRunner.canUseRoot()) {
                if (LOG_ENABLED) {
                    Log.d(TAG, "root available; enabling adb on boot");
                }
                if (lockedBoot) {
                    AdbWifiController.enableSilentlyWithPort(BuildConfig.COMPILE_ADB_PORT);
                } else {
                    AdbWifiController.enableSilently(context);
                }
            } else {
                if (LOG_ENABLED) {
                    Log.w(TAG, "root not available; cannot enable adb on boot");
                }
            }
        }
        if (lockedBoot) return;
        if (BuildConfig.FORCE_PERSISTENT_NOTIFICATION) {
            Settings.clearLastNotif(context);
            NotifyNotificationHelper.notifyStatus(context, true);
        }
    }
}
