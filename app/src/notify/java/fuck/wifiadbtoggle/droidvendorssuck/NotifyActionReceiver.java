package fuck.wifiadbtoggle.droidvendorssuck;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotifyActionReceiver extends BroadcastReceiver {
    public static final String ACTION_TOGGLE = "fuck.wifiadbtoggle.droidvendorssuck.action.TOGGLE";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        if (!ACTION_TOGGLE.equals(action)) {
            NotificationHelper.notifyStatus(context);
            return;
        }
        performToggle(context);
    }

    private void performToggle(Context context) {
        if (!ShellRunner.canUseRoot()) {
            ShellRunner.requestRoot(context);
            if (!ShellRunner.canUseRoot()) {
                ToastUtils.showShort(context, context.getString(R.string.toast_action_requires_root));
                return;
            }
        }
        AdbWifiController.toggle(context);
        NotificationHelper.notifyStatus(context);
    }

    
}
