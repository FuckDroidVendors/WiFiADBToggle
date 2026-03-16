package fuck.wifiadbtoggle.droidvendorssuck;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class MediaButtonService extends Service {
    public static final String ACTION_MEDIA_BUTTON_EVENT = "fuck.wifiadbtoggle.droidvendorssuck.action.MEDIA_BUTTON_EVENT";
    public static final String EXTRA_EVENT_NAME = "extra_event_name";
    public static final String EXTRA_EVENT_ACTION = "extra_event_action";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void start(Context context) {
        // No-op in this flavor.
    }

    public static void stop(Context context) {
        // No-op in this flavor.
    }
}
