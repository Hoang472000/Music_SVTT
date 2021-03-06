package com.example.music;

import static com.example.music.Key.CHANNEL_ID;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class MusicApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    // tao kenh thong bao
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(CHANNEL_ID
                            ,"Channel Music"
                            , NotificationManager.IMPORTANCE_DEFAULT);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

}
