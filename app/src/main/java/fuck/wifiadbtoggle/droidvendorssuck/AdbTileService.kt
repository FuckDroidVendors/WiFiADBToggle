package fuck.wifiadbtoggle.droidvendorssuck

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import android.annotation.SuppressLint
import androidx.annotation.RequiresApi

@RequiresApi(24)
class AdbTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        if (!BuildConfig.FEATURE_TILE) return
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (!BuildConfig.FEATURE_TILE) return
        if (!ShellRunner.canUseRoot()) {
            ShellRunner.requestRoot(this)
            if (!ShellRunner.canUseRoot()) {
                Toast.makeText(this, getString(R.string.toast_open_app_permission), Toast.LENGTH_SHORT).show()
            }
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                if (android.os.Build.VERSION.SDK_INT >= 34) {
                    startActivityAndCollapse34(intent)
                } else {
                    startActivityAndCollapseLegacy(intent)
                }
            }
            return
        }
        AdbWifiController.toggle(this)
        updateTile()
    }

    private fun updateTile() {
        if (!BuildConfig.FEATURE_TILE) return
        val tile = qsTile ?: return
        val enabled = AdbWifiController.isEnabled(this)
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        val ip = NetworkUtils.getActiveIp(this)
        val port = Settings.getAdbPort(this)
        tile.label = getString(R.string.tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile)
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            tile.subtitle = if (ip != null) {
                getString(R.string.ip_with_port, NetworkUtils.formatHostForPort(ip), port)
            } else {
                getString(R.string.tile_subtitle_no_ip)
            }
        }
        tile.updateTile()
    }

    @RequiresApi(34)
    private fun startActivityAndCollapse34(intent: Intent) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE
        )
        startActivityAndCollapse(pendingIntent)
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun startActivityAndCollapseLegacy(intent: Intent) {
        @Suppress("DEPRECATION")
        startActivityAndCollapse(intent)
    }
}
