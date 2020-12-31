package com.RockLinker.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import com.RockLinker.MainActivity;

public class HeadphoneButtonService extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            assert event != null;
            if (event.getAction() == KeyEvent.ACTION_UP) {
                context.sendBroadcast(new Intent("broadcastAction")
                        .putExtra("actionName", "play_pause"));
            }
        }
    }
}
