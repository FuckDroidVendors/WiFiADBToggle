package fuck.wifiadbtoggle.droidvendorssuck;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;

public class NotifyLauncherActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        QuickControlService.start(this);
        if (Build.VERSION.SDK_INT >= 21) {
            finishAndRemoveTask();
        } else {
            finish();
        }
    }
}
