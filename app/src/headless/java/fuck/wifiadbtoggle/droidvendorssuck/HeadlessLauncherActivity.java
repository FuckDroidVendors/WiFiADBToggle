package fuck.wifiadbtoggle.droidvendorssuck;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;

public class HeadlessLauncherActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!ShellRunner.canUseRoot()) {
            ShellRunner.requestRoot(this);
        }
        if (ShellRunner.canUseRoot()) {
            AdbWifiController.enable(this);
        } else {
            ToastUtils.showShort(this, getString(R.string.toast_action_requires_root));
        }
        if (Build.VERSION.SDK_INT >= 21) {
            finishAndRemoveTask();
        } else {
            finish();
        }
    }
}
