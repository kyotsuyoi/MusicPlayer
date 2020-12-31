package com.RockLinker.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class HeadphonePlugService extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_HEADSET_PLUG.equals(intent.getAction())){
            int state = intent.getIntExtra("state", -1);
            if(state==0){
                context.sendBroadcast(new Intent("broadcastAction")
                        .putExtra("actionName", "pause"));
            }
        }
    }
}
