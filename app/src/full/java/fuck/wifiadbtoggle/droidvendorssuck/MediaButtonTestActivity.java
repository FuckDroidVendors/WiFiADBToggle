package fuck.wifiadbtoggle.droidvendorssuck;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class MediaButtonTestActivity extends Activity {
    private TextView eventText;
    private TextView countText;
    private int count = 0;
    private boolean receiverRegistered = false;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String name = intent != null ? intent.getStringExtra(MediaButtonService.EXTRA_EVENT_NAME) : null;
            String action = intent != null ? intent.getStringExtra(MediaButtonService.EXTRA_EVENT_ACTION) : null;
            if (name == null) name = "unknown";
            if (action == null) action = "?";
            count++;
            eventText.setText(getString(R.string.media_test_event, name + " " + action));
            countText.setText(getString(R.string.media_test_count, count));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_button_test);
        eventText = findViewById(R.id.mediaTestEvent);
        countText = findViewById(R.id.mediaTestCount);
        Button close = findViewById(R.id.mediaTestClose);
        close.setOnClickListener(v -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(MediaButtonService.ACTION_MEDIA_BUTTON_EVENT);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            @SuppressWarnings("deprecation")
            Intent ignored = registerReceiver(receiver, filter);
        }
        receiverRegistered = true;
    }

    @Override
    protected void onStop() {
        if (receiverRegistered) {
            unregisterReceiver(receiver);
            receiverRegistered = false;
        }
        super.onStop();
    }
}
