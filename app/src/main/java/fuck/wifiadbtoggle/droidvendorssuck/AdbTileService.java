package fuck.wifiadbtoggle.droidvendorssuck;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

@TargetApi(24)
public class AdbTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        if (!BuildConfig.FEATURE_TILE) return;
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        if (!BuildConfig.FEATURE_TILE) return;
        if (!ShellRunner.canUseRoot()) {
            ShellRunner.requestRoot(this);
            if (!ShellRunner.canUseRoot()) {
                Toast.makeText(this, getString(R.string.toast_open_app_permission), Toast.LENGTH_SHORT).show();
            }
            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (intent != null) {
                if (Build.VERSION.SDK_INT >= 34) {
                    startActivityAndCollapse34(intent);
                } else {
                    startActivityAndCollapseLegacy(intent);
                }
            }
            return;
        }
        AdbWifiController.toggle(this);
        updateTile();
    }

    private void updateTile() {
        if (!BuildConfig.FEATURE_TILE) return;
        Tile tile = getQsTile();
        if (tile == null) return;
        boolean enabled = AdbWifiController.isEnabled(this);
        tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        NetworkUtils.IpResult ip = NetworkUtils.getActiveIp(this);
        int port = Settings.getAdbPort(this);
        tile.setLabel(getString(R.string.tile_label));
        tile.setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher));
        if (Build.VERSION.SDK_INT >= 29) {
            tile.setSubtitle(ip != null
                ? getString(R.string.ip_with_port, NetworkUtils.formatHostForPort(ip), port)
                : getString(R.string.tile_subtitle_no_ip));
        }
        tile.updateTile();
    }

    @TargetApi(34)
    private void startActivityAndCollapse34(Intent intent) {
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE
        );
        startActivityAndCollapse(pendingIntent);
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private void startActivityAndCollapseLegacy(Intent intent) {
        @SuppressWarnings("deprecation")
        Intent launchIntent = intent;
        startActivityAndCollapse(launchIntent);
    }
}
