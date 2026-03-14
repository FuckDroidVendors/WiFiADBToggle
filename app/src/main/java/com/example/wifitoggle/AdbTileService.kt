package com.example.wifitoggle

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class AdbTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (!ShellRunner.canUseRoot() && !ShellRunner.canUseShizuku()) {
            Toast.makeText(this, "Open app to grant Shizuku or root", Toast.LENGTH_SHORT).show()
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            }
            return
        }
        AdbWifiController.toggle(this)
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val enabled = AdbWifiController.isEnabled(this)
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        val ip = NetworkUtils.getLocalIpV4()
        tile.label = "ADB"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile)
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            tile.subtitle = if (ip != null) "$ip:5555" else "no IP"
        }
        tile.updateTile()
    }
}
