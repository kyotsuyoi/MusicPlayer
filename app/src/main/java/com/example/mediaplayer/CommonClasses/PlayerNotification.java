package com.example.mediaplayer.CommonClasses;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.mediaplayer.MainActivity;
import com.example.mediaplayer.R;

public class PlayerNotification {

    private static final com.example.mediaplayer.CommonClasses.Handler Handler =
            new com.example.mediaplayer.CommonClasses.Handler();

    public static final String CHANNEL_ID = "channel";

    public static final String ACTION_PREVIOUS = "previous";
    public static final String ACTION_PLAY = "play";
    public static final String ACTION_NEXT = "next";

    public static Notification notification;

    public static void createNotification(Context context){

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
            MediaSessionCompat mediaSessionCompat = new MediaSessionCompat(context, "tag");

            Bitmap art = Handler.ImageDecode(MainActivity.musicInfo.get("art").getAsString());

            int play = R.drawable.ic_play_arrow_10dp;
            if(MainActivity.isPlaying()){
                play = R.drawable.ic_pause_10dp;
            }

            PendingIntent pendingIntentPrevious, pendingIntentPlay, pendingIntentNext;

            Intent intentPrevious = new Intent(context, NotificationActionService.class)
                    .setAction(ACTION_PREVIOUS);
            pendingIntentPrevious = PendingIntent.getBroadcast(context, 0,
                    intentPrevious, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent intentPlay = new Intent(context, NotificationActionService.class)
                    .setAction(ACTION_PLAY);
            pendingIntentPlay = PendingIntent.getBroadcast(context, 0,
                    intentPlay, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent intentNext = new Intent(context, NotificationActionService.class)
                    .setAction(ACTION_NEXT);
            pendingIntentNext = PendingIntent.getBroadcast(context, 0,
                    intentNext, PendingIntent.FLAG_UPDATE_CURRENT);

            notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.icon)
                    .setContentTitle(MainActivity.musicInfo.get("title").getAsString())
                    .setContentText(MainActivity.musicInfo.get("artist").getAsString())
                    .setLargeIcon(art)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(false)
                    .addAction(R.drawable.ic_skip_previous_10dp, "Previous", pendingIntentPrevious)
                    .addAction(play, "Play", pendingIntentPlay)
                    .addAction(R.drawable.ic_skip_next_10dp, "Next", pendingIntentNext)
                    .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setShowActionsInCompactView(0,1,2)
                            .setMediaSession(mediaSessionCompat.getSessionToken())
                    )
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();

            notificationManagerCompat.notify(1, notification);
        }
    }
}
