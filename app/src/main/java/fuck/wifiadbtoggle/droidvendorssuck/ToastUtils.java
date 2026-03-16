package fuck.wifiadbtoggle.droidvendorssuck;

import android.content.Context;
import android.widget.Toast;

public final class ToastUtils {
    private ToastUtils() {
    }

    public static void showShort(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
