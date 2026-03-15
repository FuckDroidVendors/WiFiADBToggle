package fuck.wifiadbtoggle.droidvendorssuck

import android.content.Context
import android.widget.Toast

object ToastUtils {
    fun showShort(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
