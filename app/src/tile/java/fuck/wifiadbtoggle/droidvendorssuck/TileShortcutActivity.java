package fuck.wifiadbtoggle.droidvendorssuck;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;

public class TileShortcutActivity extends Activity {
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
        if (!ensureRoot()) return;
        AdbWifiController.toggle(this);
    }

    private void performEnable() {
        if (!ensureRoot()) return;
        AdbWifiController.enable(this);
    }

    private void performDisable() {
        if (!ensureRoot()) return;
        AdbWifiController.disable(this);
    }

    private void openSettings() {
        if (!ShellRunner.canUseRoot()) {
            ShellRunner.requestRoot(this);
        }
        ToastUtils.showShort(this, getString(R.string.toast_add_tile_hint));
    }

    private boolean ensureRoot() {
        if (!ShellRunner.canUseRoot()) {
            ShellRunner.requestRoot(this);
            if (!ShellRunner.canUseRoot()) {
                ToastUtils.showShort(this, getString(R.string.toast_action_requires_root));
                return false;
            }
        }
        return true;
    }
}
