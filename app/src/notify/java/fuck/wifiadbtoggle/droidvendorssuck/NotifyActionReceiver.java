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
        launchToggleActivity(context);
    }

    private void launchToggleActivity(Context context) {
        Intent toggleIntent = new Intent(context, ShortcutActivity.class)
            .setAction(ShortcutActivity.ACTION_TOGGLE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(toggleIntent);
    }
}
