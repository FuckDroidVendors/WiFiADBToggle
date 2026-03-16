package fuck.wifiadbtoggle.droidvendorssuck;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ShortcutActivity extends Activity {
    public static final String ACTION_TOGGLE = "fuck.wifiadbtoggle.droidvendorssuck.action.TOGGLE";
    public static final String ACTION_ENABLE = "fuck.wifiadbtoggle.droidvendorssuck.action.ENABLE";
    public static final String ACTION_DISABLE = "fuck.wifiadbtoggle.droidvendorssuck.action.DISABLE";
    public static final String ACTION_SETTINGS = "fuck.wifiadbtoggle.droidvendorssuck.action.SETTINGS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String action = getIntent() != null ? getIntent().getAction() : null;
        if (ACTION_TOGGLE.equals(action)) {
            performToggle();
        } else if (ACTION_ENABLE.equals(action)) {
            performEnable();
        } else if (ACTION_DISABLE.equals(action)) {
            performDisable();
        } else {
            openSettings();
        }
        finish();
    }

    private void performToggle() {
        if (!ShellRunner.canUseRoot()) {
            ShellRunner.requestRoot(this);
            if (!ShellRunner.canUseRoot()) {
                ToastUtils.showShort(this, getString(R.string.toast_action_requires_root));
                return;
            }
        }
        AdbWifiController.toggle(this);
        ScheduleManager.applyScheduleNow(this);
        NotificationHelper.notifyStatus(this);
    }

    private void performEnable() {
        if (!ShellRunner.canUseRoot()) {
            ShellRunner.requestRoot(this);
            if (!ShellRunner.canUseRoot()) {
                ToastUtils.showShort(this, getString(R.string.toast_action_requires_root));
                return;
            }
        }
        AdbWifiController.enable(this);
        ScheduleManager.applyScheduleNow(this);
        NotificationHelper.notifyStatus(this);
    }

    private void performDisable() {
        if (!ShellRunner.canUseRoot()) {
            ShellRunner.requestRoot(this);
            if (!ShellRunner.canUseRoot()) {
                ToastUtils.showShort(this, getString(R.string.toast_action_requires_root));
                return;
            }
        }
        AdbWifiController.disable(this);
        ScheduleManager.applyScheduleNow(this);
        NotificationHelper.notifyStatus(this);
    }

    private void openSettings() {
        if (BuildConfig.FORCE_PERSISTENT_NOTIFICATION) {
            QuickControlService.start(this);
            return;
        }
        Intent intent = new Intent().setClassName(
            this,
            "fuck.wifiadbtoggle.droidvendorssuck.MainActivity"
        );
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
