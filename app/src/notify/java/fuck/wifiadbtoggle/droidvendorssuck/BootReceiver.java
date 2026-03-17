package fuck.wifiadbtoggle.droidvendorssuck;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        boolean lockedBoot = Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action);
        boolean normalBoot = Intent.ACTION_BOOT_COMPLETED.equals(action);
        if (!lockedBoot && !normalBoot) return;
        if (lockedBoot && !BuildConfig.LOCKED_BOOT_ENABLED) return;

        if (BuildConfig.AUTOBOOT_ENABLE_ADB && !BuildConfig.SETTINGS_UI) {
            if (ShellRunner.canUseRoot()) {
                if (lockedBoot) {
                    AdbWifiController.enableSilentlyWithPort(BuildConfig.COMPILE_ADB_PORT);
                } else {
                    AdbWifiController.enableSilently(context);
                }
            }
        }
        if (lockedBoot) return;
        if (BuildConfig.FORCE_PERSISTENT_NOTIFICATION) {
            NotificationHelper.notifyStatus(context);
        }
    }
}
