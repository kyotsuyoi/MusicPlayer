package com.example.mediaplayer.CommonClasses;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.widget.Toast;

public class AudioManagerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_MEDIA_BUTTON != intent.getAction()){
            return;
        }

        KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            Toast.makeText(context, event.getKeyCode(),Toast.LENGTH_LONG).show();
        }

    }
}
