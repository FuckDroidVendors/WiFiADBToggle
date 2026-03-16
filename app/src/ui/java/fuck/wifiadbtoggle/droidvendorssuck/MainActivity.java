package fuck.wifiadbtoggle.droidvendorssuck;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private TextView statusText;
    private Switch autoStartSwitch;
    private Switch keepAwakeSwitch;
    private Switch keepScreenOnSwitch;
    private Switch autoEnableEthernetSwitch;
    private Switch disableOnDisconnectSwitch;
    private Switch autoEnableSsidSwitch;
    private Switch filterBssidSwitch;
    private EditText ssidListEdit;
    private EditText bssidListEdit;
    private Button addCurrentWifiButton;
    private Switch mediaButtonsSwitch;
    private Button mediaTestButton;
    private Switch persistentNotificationSwitch;
    private Switch connectionNotificationSwitch;
    private EditText adbPortEdit;
    private RadioGroup mediaPatternGroup;
    private RadioButton mediaPatternSingle;
    private RadioButton mediaPatternDouble;
    private RadioButton mediaPatternTriple;
    private RadioButton mediaPatternLong;
    private Switch scheduleEnabledSwitch;
    private Button openScheduleButton;
    private final Handler monitorHandler = new Handler(Looper.getMainLooper());
    private final Runnable monitorRestartRunnable = new Runnable() {
        @Override
        public void run() {
            NetworkMonitorService.start(MainActivity.this);
        }
    };

    private boolean awaitingLocationPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BuildConfig.FORCE_PERSISTENT_NOTIFICATION) {
            QuickControlService.start(this);
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        Button toggle = findViewById(R.id.testToggle);
        Button closeApp = findViewById(R.id.closeApp);
        autoStartSwitch = findViewById(R.id.settingAutoStart);
        keepAwakeSwitch = findViewById(R.id.settingKeepAwake);
        keepScreenOnSwitch = findViewById(R.id.settingKeepScreenOn);
        autoEnableEthernetSwitch = findViewById(R.id.settingAutoEnableEthernet);
        disableOnDisconnectSwitch = findViewById(R.id.settingDisableOnDisconnect);
        autoEnableSsidSwitch = findViewById(R.id.settingAutoEnableSsid);
        filterBssidSwitch = findViewById(R.id.settingFilterBssid);
        ssidListEdit = findViewById(R.id.settingSsidList);
        bssidListEdit = findViewById(R.id.settingBssidList);
        addCurrentWifiButton = findViewById(R.id.addCurrentWifi);
        mediaButtonsSwitch = findViewById(R.id.settingMediaButtons);
        mediaTestButton = findViewById(R.id.openMediaTest);
        persistentNotificationSwitch = findViewById(R.id.settingPersistentNotification);
        connectionNotificationSwitch = findViewById(R.id.settingConnectionNotification);
        adbPortEdit = findViewById(R.id.settingAdbPort);
        mediaPatternGroup = findViewById(R.id.settingMediaPatternGroup);
        mediaPatternSingle = findViewById(R.id.mediaPatternSingle);
        mediaPatternDouble = findViewById(R.id.mediaPatternDouble);
        mediaPatternTriple = findViewById(R.id.mediaPatternTriple);
        mediaPatternLong = findViewById(R.id.mediaPatternLong);
        scheduleEnabledSwitch = findViewById(R.id.settingScheduleEnabled);
        openScheduleButton = findViewById(R.id.openSchedule);

        if (!BuildConfig.FEATURE_MONITOR) {
            autoStartSwitch.setVisibility(View.GONE);
            keepAwakeSwitch.setVisibility(View.GONE);
            keepScreenOnSwitch.setVisibility(View.GONE);
            autoEnableEthernetSwitch.setVisibility(View.GONE);
            disableOnDisconnectSwitch.setVisibility(View.GONE);
            autoEnableSsidSwitch.setVisibility(View.GONE);
            ssidListEdit.setVisibility(View.GONE);
            addCurrentWifiButton.setVisibility(View.GONE);
            filterBssidSwitch.setVisibility(View.GONE);
            bssidListEdit.setVisibility(View.GONE);
        }

        if (!BuildConfig.FEATURE_MEDIA) {
            mediaButtonsSwitch.setVisibility(View.GONE);
            mediaTestButton.setVisibility(View.GONE);
            findViewById(R.id.settingMediaPatternLabel).setVisibility(View.GONE);
            mediaPatternGroup.setVisibility(View.GONE);
        }

        if (!BuildConfig.FEATURE_SCHEDULE) {
            scheduleEnabledSwitch.setVisibility(View.GONE);
            openScheduleButton.setVisibility(View.GONE);
        }

        if (!BuildConfig.FEATURE_NOTIFICATION) {
            persistentNotificationSwitch.setVisibility(View.GONE);
            connectionNotificationSwitch.setVisibility(View.GONE);
        } else if (!BuildConfig.FEATURE_CONNECTIONS) {
            connectionNotificationSwitch.setVisibility(View.GONE);
        }

        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ShellRunner.canUseRoot()) {
                    ShellRunner.requestRoot(MainActivity.this);
                }
                if (ShellRunner.canUseRoot()) {
                    AdbWifiController.toggle(MainActivity.this);
                    ScheduleManager.applyScheduleNow(MainActivity.this);
                    updateStatus();
                }
            }
        });
        closeApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 21) {
                    finishAndRemoveTask();
                } else {
                    finish();
                }
                finishAffinity();
            }
        });

        bindSettings();
        updateStatus();
        ScheduleAlarmScheduler.scheduleNext(this);
    }

    @Override
    protected void onDestroy() {
        monitorHandler.removeCallbacks(monitorRestartRunnable);
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isChangingConfigurations()) return;
        if (Settings.isKeepScreenOnEnabled(this)) return;
        finish();
    }

    private void updateStatus() {
        boolean root = ShellRunner.canUseRoot();
        boolean enabled = AdbWifiController.isEnabled(this);
        NetworkUtils.IpResult ip = NetworkUtils.getActiveIp(this);
        String yes = getString(R.string.value_yes);
        String no = getString(R.string.value_no);
        String on = getString(R.string.value_on);
        String off = getString(R.string.value_off);
        List<String> lines = new ArrayList<>();
        lines.add(getString(R.string.status_root, root ? yes : no));
        lines.add(getString(R.string.status_wifi_adb, enabled ? on : off));
        if (BuildConfig.FEATURE_NOTIFICATION || BuildConfig.FEATURE_TILE) {
            String ipText;
            if (ip != null) {
                int port = Settings.getAdbPort(this);
                ipText = getString(R.string.ip_with_port, NetworkUtils.formatHostForPort(ip), port);
            } else {
                ipText = getString(R.string.value_no_ip);
            }
            lines.add(getString(R.string.status_ip, ipText));
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(lines.get(i));
        }
        statusText.setText(sb.toString());
    }

    private void bindSettings() {
        if (BuildConfig.FEATURE_MONITOR) {
            autoStartSwitch.setChecked(Settings.isAutoStartEnabled(this));
            keepAwakeSwitch.setChecked(Settings.isKeepAwakeEnabled(this));
            keepScreenOnSwitch.setChecked(Settings.isKeepScreenOnEnabled(this));
            autoEnableEthernetSwitch.setChecked(Settings.isAutoEnableEthernetEnabled(this));
            disableOnDisconnectSwitch.setChecked(Settings.isDisableOnDisconnectEnabled(this));
            autoEnableSsidSwitch.setChecked(Settings.isAutoEnableSsidEnabled(this));
            filterBssidSwitch.setChecked(Settings.isFilterBssidEnabled(this));
            ssidListEdit.setText(Settings.getSsidList(this));
            bssidListEdit.setText(Settings.getBssidList(this));
            applyKeepScreenOn(Settings.isKeepScreenOnEnabled(this));
        }
        if (BuildConfig.FEATURE_MEDIA) {
            mediaButtonsSwitch.setChecked(Settings.isMediaButtonsEnabled(this));
        }
        if (BuildConfig.FEATURE_NOTIFICATION) {
            persistentNotificationSwitch.setChecked(Settings.isPersistentNotificationEnabled(this));
            if (BuildConfig.FEATURE_CONNECTIONS) {
                connectionNotificationSwitch.setChecked(Settings.isConnectionNotificationEnabled(this));
            }
        }
        adbPortEdit.setText(String.valueOf(Settings.getAdbPort(this)));
        if (BuildConfig.FEATURE_SCHEDULE) {
            scheduleEnabledSwitch.setChecked(Settings.isScheduleEnabled(this));
        }
        if (BuildConfig.FEATURE_MEDIA) {
            updateMediaPatternVisibility(mediaButtonsSwitch.isChecked());
        }

        if (BuildConfig.FEATURE_MEDIA) {
            switch (Settings.getMediaPattern(this)) {
                case SINGLE:
                    mediaPatternSingle.setChecked(true);
                    break;
                case DOUBLE:
                    mediaPatternDouble.setChecked(true);
                    break;
                case TRIPLE:
                    mediaPatternTriple.setChecked(true);
                    break;
                case LONG:
                    mediaPatternLong.setChecked(true);
                    break;
                default:
                    break;
            }
        }

        if (BuildConfig.FEATURE_MONITOR) {
            autoStartSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Settings.setAutoStartEnabled(MainActivity.this, isChecked);
                if (isChecked) {
                    NetworkMonitorService.start(MainActivity.this);
                    if (BuildConfig.FEATURE_NOTIFICATION && Settings.isPersistentNotificationEnabled(MainActivity.this)) {
                        QuickControlService.stop(MainActivity.this);
                    }
                    ToastUtils.showShort(MainActivity.this, getString(R.string.toast_monitor_started));
                } else {
                    NetworkMonitorService.stop(MainActivity.this);
                    if (BuildConfig.FEATURE_NOTIFICATION && Settings.isPersistentNotificationEnabled(MainActivity.this)) {
                        QuickControlService.start(MainActivity.this);
                    }
                    ToastUtils.showShort(MainActivity.this, getString(R.string.toast_monitor_stopped));
                }
            });
        }

        if (BuildConfig.FEATURE_MONITOR) {
            keepAwakeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Settings.setKeepAwakeEnabled(MainActivity.this, isChecked);
                scheduleMonitorRestart();
            });

            keepScreenOnSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Settings.setKeepScreenOnEnabled(MainActivity.this, isChecked);
                applyKeepScreenOn(isChecked);
            });

            autoEnableEthernetSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Settings.setAutoEnableEthernetEnabled(MainActivity.this, isChecked);
                scheduleMonitorRestart();
            });

            disableOnDisconnectSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Settings.setDisableOnDisconnectEnabled(MainActivity.this, isChecked);
                scheduleMonitorRestart();
            });

            autoEnableSsidSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Settings.setAutoEnableSsidEnabled(MainActivity.this, isChecked);
                scheduleMonitorRestart();
            });

            filterBssidSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Settings.setFilterBssidEnabled(MainActivity.this, isChecked);
                scheduleMonitorRestart();
            });

            ssidListEdit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    Settings.setSsidList(MainActivity.this, s != null ? s.toString() : "");
                    scheduleMonitorRestart();
                }
            });

            bssidListEdit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    Settings.setBssidList(MainActivity.this, s != null ? s.toString() : "");
                    scheduleMonitorRestart();
                }
            });

            addCurrentWifiButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (hasFineLocationPermission()) {
                        addCurrentWifi();
                    } else {
                        requestFineLocationPermission();
                    }
                }
            });
        }

        if (BuildConfig.FEATURE_MEDIA) {
            mediaButtonsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Settings.setMediaButtonsEnabled(MainActivity.this, isChecked);
                updateMediaPatternVisibility(isChecked);
                if (isChecked) {
                    MediaButtonService.start(MainActivity.this);
                    ToastUtils.showShort(MainActivity.this, getString(R.string.toast_media_listener_started));
                } else {
                    MediaButtonService.stop(MainActivity.this);
                    ToastUtils.showShort(MainActivity.this, getString(R.string.toast_media_listener_stopped));
                }
            });
        }

        if (BuildConfig.FEATURE_MEDIA) {
            mediaTestButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent().setClassName(
                        MainActivity.this,
                        "fuck.wifiadbtoggle.droidvendorssuck.MediaButtonTestActivity"
                    );
                    startActivity(intent);
                }
            });
        }

        if (BuildConfig.FEATURE_NOTIFICATION) {
            persistentNotificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Settings.setPersistentNotificationEnabled(MainActivity.this, isChecked);
                if (isChecked) {
                    if (BuildConfig.FEATURE_MONITOR &&
                        Settings.isAutoStartEnabled(MainActivity.this) &&
                        Settings.isAnyMonitorRuleEnabled(MainActivity.this)
                    ) {
                        NetworkMonitorService.start(MainActivity.this);
                        QuickControlService.stop(MainActivity.this);
                    } else {
                        QuickControlService.start(MainActivity.this);
                    }
                } else {
                    QuickControlService.stop(MainActivity.this);
                    if (BuildConfig.FEATURE_MONITOR &&
                        Settings.isAutoStartEnabled(MainActivity.this) &&
                        Settings.isAnyMonitorRuleEnabled(MainActivity.this)
                    ) {
                        NetworkMonitorService.start(MainActivity.this);
                    } else {
                        NotificationHelper.cancelStatus(MainActivity.this);
                    }
                }
            });
        }

        if (BuildConfig.FEATURE_NOTIFICATION && BuildConfig.FEATURE_CONNECTIONS) {
            connectionNotificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Settings.setConnectionNotificationEnabled(MainActivity.this, isChecked);
                if (isChecked) {
                    NotificationHelper.notifyConnections(MainActivity.this);
                } else {
                    NotificationHelper.cancelConnections(MainActivity.this);
                }
            });
        }

        adbPortEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s == null) return;
                String value = s.toString().trim();
                if (value.isEmpty()) return;
                int port;
                try {
                    port = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    return;
                }
                if (port < 1 || port > 65535) return;
                Settings.setAdbPort(MainActivity.this, port);
                if (AdbWifiController.isEnabled(MainActivity.this)) {
                    AdbWifiController.applyPort(MainActivity.this, port);
                }
                updateStatus();
                NotificationHelper.notifyStatus(MainActivity.this);
            }
        });

        if (BuildConfig.FEATURE_SCHEDULE) {
            scheduleEnabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Settings.setScheduleEnabled(MainActivity.this, isChecked);
                if (isChecked) {
                    ScheduleManager.applyScheduleNow(MainActivity.this);
                }
                ScheduleAlarmScheduler.scheduleNext(MainActivity.this);
            });
        }

        if (BuildConfig.FEATURE_MEDIA) {
            mediaPatternGroup.setOnCheckedChangeListener((group, checkedId) -> {
                MediaPattern pattern;
                if (checkedId == R.id.mediaPatternSingle) {
                    pattern = MediaPattern.SINGLE;
                } else if (checkedId == R.id.mediaPatternDouble) {
                    pattern = MediaPattern.DOUBLE;
                } else if (checkedId == R.id.mediaPatternTriple) {
                    pattern = MediaPattern.TRIPLE;
                } else if (checkedId == R.id.mediaPatternLong) {
                    pattern = MediaPattern.LONG;
                } else {
                    pattern = MediaPattern.DOUBLE;
                }
                Settings.setMediaPattern(MainActivity.this, pattern);
            });
        }

        if (BuildConfig.FEATURE_SCHEDULE) {
            openScheduleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent().setClassName(
                        MainActivity.this,
                        "fuck.wifiadbtoggle.droidvendorssuck.ScheduleActivity"
                    );
                    startActivity(intent);
                }
            });
        }
    }

    private void scheduleMonitorRestart() {
        if (!BuildConfig.FEATURE_MONITOR) return;
        monitorHandler.removeCallbacks(monitorRestartRunnable);
        if (!Settings.isAutoStartEnabled(this) || !Settings.isAnyMonitorRuleEnabled(this)) {
            NetworkMonitorService.stop(this);
            return;
        }
        monitorHandler.postDelayed(monitorRestartRunnable, 400L);
    }

    private void updateMediaPatternVisibility(boolean enabled) {
        if (!BuildConfig.FEATURE_MEDIA) return;
        int visibility = enabled ? View.VISIBLE : View.GONE;
        findViewById(R.id.settingMediaPatternLabel).setVisibility(visibility);
        mediaPatternGroup.setVisibility(visibility);
    }

    private void addCurrentWifi() {
        if (!BuildConfig.FEATURE_MONITOR) return;
        Pair<String, String> info = getCurrentWifiInfo();
        if (info == null) {
            ToastUtils.showShort(this, getString(R.string.toast_wifi_info_unavailable));
            return;
        }
        Settings.addSsid(this, info.first);
        ssidListEdit.setText(Settings.getSsidList(this));
        if (info.second != null) {
            Settings.addBssid(this, info.second);
            bssidListEdit.setText(Settings.getBssidList(this));
        }
        NetworkMonitorService.start(this);
    }

    private Pair<String, String> getCurrentWifiInfo() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wifiManager == null) return null;
            android.net.wifi.WifiInfo info = wifiManager.getConnectionInfo();
            if (info == null) return null;
            String ssid = info.getSSID();
            if (ssid == null || "<unknown ssid>".equals(ssid)) return null;
            return Pair.create(ssid, info.getBSSID());
        } catch (Exception e) {
            return null;
        }
    }

    private void applyKeepScreenOn(boolean enabled) {
        if (enabled) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private boolean hasFineLocationPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestFineLocationPermission() {
        if (Build.VERSION.SDK_INT < 23) {
            addCurrentWifi();
            return;
        }
        if (awaitingLocationPermission) return;
        awaitingLocationPermission = true;
        requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, REQUEST_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_LOCATION) return;
        awaitingLocationPermission = false;
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (granted) {
            addCurrentWifi();
        } else {
            ToastUtils.showShort(this, getString(R.string.toast_location_permission_required));
        }
    }

    private static final int REQUEST_LOCATION = 1001;
}
