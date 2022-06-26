package com.example.music.broadcast;

import static com.example.music.Key.CHANNEL_ID;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.music.Key;
import com.example.music.R;

public class MusicReceiver extends BroadcastReceiver {

    private static int NOTIFICATION_RECEIVER_ID = 2;
    protected String mTitle;
    protected String mSubtitle;

    public MusicReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Key.ACTION_PLAY_NEXT_SONG)) {
            mTitle = intent.getStringExtra(Key.CONST_TITLE);
            mSubtitle = intent.getStringExtra(Key.CONST_SUBTITLE);
        } else {
            mTitle = "Service";
            mSubtitle = "Service chưa được khởi tạo";
        }
        createNotification(context, mTitle, mSubtitle);
    }

    private void createNotification(Context context, String title, String subtitle) {
        Notification notificationMusic = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.splash_play_music_192)
                .setContentTitle(title)
                .setContentText(subtitle)
                .build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(NOTIFICATION_RECEIVER_ID, notificationMusic);

        NOTIFICATION_RECEIVER_ID++;
    }
}