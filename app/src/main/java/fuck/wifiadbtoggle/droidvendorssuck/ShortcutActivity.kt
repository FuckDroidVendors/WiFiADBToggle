package fuck.wifiadbtoggle.droidvendorssuck

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class ShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent?.action) {
            ACTION_TOGGLE -> performToggle()
            ACTION_ENABLE -> performEnable()
            ACTION_DISABLE -> performDisable()
            ACTION_SETTINGS -> openSettings()
            else -> openSettings()
        }
        finish()
    }

    private fun performToggle() {
        if (!ShellRunner.canUseRoot()) {
            ShellRunner.requestRoot(this)
            if (!ShellRunner.canUseRoot()) {
                ToastUtils.showShort(this, getString(R.string.toast_action_requires_root))
                return
            }
        }
        AdbWifiController.toggle(this)
        ScheduleManager.applyScheduleNow(this)
    }

    private fun performEnable() {
        if (!ShellRunner.canUseRoot()) {
            ShellRunner.requestRoot(this)
            if (!ShellRunner.canUseRoot()) {
                ToastUtils.showShort(this, getString(R.string.toast_action_requires_root))
                return
            }
        }
        AdbWifiController.enable(this)
        ScheduleManager.applyScheduleNow(this)
    }

    private fun performDisable() {
        if (!ShellRunner.canUseRoot()) {
            ShellRunner.requestRoot(this)
            if (!ShellRunner.canUseRoot()) {
                ToastUtils.showShort(this, getString(R.string.toast_action_requires_root))
                return
            }
        }
        AdbWifiController.disable(this)
        ScheduleManager.applyScheduleNow(this)
    }

    private fun openSettings() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    companion object {
        const val ACTION_TOGGLE = "fuck.wifiadbtoggle.droidvendorssuck.action.TOGGLE"
        const val ACTION_ENABLE = "fuck.wifiadbtoggle.droidvendorssuck.action.ENABLE"
        const val ACTION_DISABLE = "fuck.wifiadbtoggle.droidvendorssuck.action.DISABLE"
        const val ACTION_SETTINGS = "fuck.wifiadbtoggle.droidvendorssuck.action.SETTINGS"
    }
}
