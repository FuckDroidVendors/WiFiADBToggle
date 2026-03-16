package fuck.wifiadbtoggle.droidvendorssuck;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.KeyEvent;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

public class MediaButtonService extends Service {
    private MediaSessionCompat mediaSession;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int pressCount = 0;
    private boolean longPressFired = false;
    private final Runnable longPressRunnable = new Runnable() {
        @Override
        public void run() {
            longPressFired = true;
            triggerAction(MediaPattern.LONG);
            resetPressState();
        }
    };
    private final Runnable finalizeRunnable = new Runnable() {
        @Override
        public void run() {
            if (pressCount == 1) {
                triggerAction(MediaPattern.SINGLE);
            } else if (pressCount == 2) {
                triggerAction(MediaPattern.DOUBLE);
            } else if (pressCount == 3) {
                triggerAction(MediaPattern.TRIPLE);
            }
            resetPressState();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mediaSession = new MediaSessionCompat(this, "adb_media_buttons");
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
                if (mediaButtonEvent != null) {
                    KeyEvent event = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                    if (event != null) handleKeyEvent(event);
                }
                return true;
            }
        });
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);
        mediaSession.setPlaybackState(
            new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
                .setState(PlaybackStateCompat.STATE_PAUSED, 0, 0f)
                .build()
        );
        mediaSession.setActive(true);
        startForeground(NOTIF_ID, buildNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!Settings.isMediaButtonsEnabled(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (intent != null) {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event != null) {
                handleKeyEvent(event);
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mediaSession.release();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleKeyEvent(KeyEvent event) {
        if (event.getKeyCode() != KeyEvent.KEYCODE_HEADSETHOOK &&
            event.getKeyCode() != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            return;
        }
        broadcastEvent(event);
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getRepeatCount() == 0) {
                pressCount++;
                if (Settings.getMediaPattern(this) == MediaPattern.LONG) {
                    handler.postDelayed(longPressRunnable, LONG_PRESS_MS);
                } else {
                    handler.removeCallbacks(finalizeRunnable);
                    handler.postDelayed(finalizeRunnable, MULTI_PRESS_WINDOW_MS);
                }
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            if (Settings.getMediaPattern(this) == MediaPattern.LONG && !longPressFired) {
                handler.removeCallbacks(longPressRunnable);
                resetPressState();
            }
        }
    }

    private void triggerAction(MediaPattern triggered) {
        MediaPattern selected = Settings.getMediaPattern(this);
        if (selected != triggered) return;
        if (!ShellRunner.canUseRoot()) {
            ToastUtils.showShort(this, getString(R.string.toast_action_requires_root));
            return;
        }
        AdbWifiController.toggle(this);
    }

    private void resetPressState() {
        pressCount = 0;
        longPressFired = false;
        handler.removeCallbacks(longPressRunnable);
        handler.removeCallbacks(finalizeRunnable);
    }

    private Notification buildNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                NOTIF_CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.notif_channel_desc));
            manager.createNotificationChannel(channel);
            return new Notification.Builder(this, NOTIF_CHANNEL_ID)
                .setContentTitle(getString(R.string.notif_title))
                .setContentText(getString(R.string.notif_text))
                .setSmallIcon(R.drawable.ic_launcher)
                .setOngoing(true)
                .build();
        }
        return new Notification.Builder(this)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .build();
    }

    public static final String ACTION_MEDIA_BUTTON_EVENT = "fuck.wifiadbtoggle.droidvendorssuck.action.MEDIA_BUTTON_EVENT";
    public static final String EXTRA_EVENT_NAME = "extra_event_name";
    public static final String EXTRA_EVENT_ACTION = "extra_event_action";
    private static final String NOTIF_CHANNEL_ID = "adb_media_buttons";
    private static final int NOTIF_ID = 2001;
    private static final long MULTI_PRESS_WINDOW_MS = 500L;
    private static final long LONG_PRESS_MS = 800L;

    public static void start(Context context) {
        if (!Settings.isMediaButtonsEnabled(context)) return;
        Intent intent = new Intent(context, MediaButtonService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, MediaButtonService.class);
        context.stopService(intent);
    }

    private void broadcastEvent(KeyEvent event) {
        String name;
        if (event.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK) {
            name = "HEADSETHOOK";
        } else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            name = "PLAY_PAUSE";
        } else {
            name = String.valueOf(event.getKeyCode());
        }
        String action;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            action = "DOWN";
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            action = "UP";
        } else {
            action = String.valueOf(event.getAction());
        }
        Intent intent = new Intent(ACTION_MEDIA_BUTTON_EVENT);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_EVENT_NAME, name);
        intent.putExtra(EXTRA_EVENT_ACTION, action);
        sendBroadcast(intent);
    }
}
