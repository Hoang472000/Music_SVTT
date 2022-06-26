package com.example.music.service;

import static com.example.music.Key.CHANNEL_ID;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.provider.MediaStore;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.music.Key;
import com.example.music.R;
import com.example.music.activity.MainActivity;
import com.example.music.database.AllSongOperations;
import com.example.music.database.FavoritesOperations;
import com.example.music.interfaces.INotification;
import com.example.music.model.Song;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;


@SuppressWarnings("ALL")
public class MusicService extends Service {

    public static final int NOTIFY_ID = 1;
    public static final int REQUEST_CODE = 1;

    public static final int CHECK_PLAY = 0;

    private int mImgLike;
    private int mImgDislike;
    private int mImgPlayPause;
    private AllSongOperations mAllSongOperations;
    private ArrayList<Song> mSongsList;

    public MediaPlayer mediaPlayer;

    private String mTitle;
    private String mSubtitle;
    private long mImage;
    private int mLike;
    private int mPlay;
    private int mPosition;

    private MainActivity mainActivity;
    private INotification mINotification;

    private boolean mIsNotification = false;
    private boolean mIsContext = false;

    private MusicBinder mBinder = new MusicBinder();

    private boolean checkOnCompletionListener = false;
    private FavoritesOperations mFavoritesOperations;

    public MusicService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mIsContext = true;
        mIsNotification = true;
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }
        mSongsList = new ArrayList<>();
        //mSongsList = mAllSongOperations.getAllSong();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class MusicBinder extends Binder {
        public MusicService getMusicService() {
            return MusicService.this;
        }
    }

    public void setINotification (INotification iNotification) {
        this.mINotification = iNotification;
    }

    public void setSonglist(ArrayList<Song> songlist){
        this.mSongsList = songlist;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        mAllSongOperations = new AllSongOperations(newBase);
        mFavoritesOperations = new FavoritesOperations(newBase);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mPosition = intent.getIntExtra(Key.KEY_POSITION, 0);

        Song song = null;
        /*if (mSongsList.size() == 0) {
            mSongsList = mAllSongOperations.getAllSong();
        }*/
        if (mSongsList.size()>0) {
            song = mSongsList.get(mPosition);
        }

        if (intent.getAction() == null){
            if (song != null) {
                sendNotification(getBaseContext(), song, mPosition);
            }
        } else {
            switch (intent.getAction()){
                case Key.ACTION_NEXT_SONG:{
                    mSongsList.get(mPosition).setPlay(Key.STOP);
                    mAllSongOperations.updateSong(mSongsList.get(mPosition));

                    int position = mPosition + 1;
                    if (position == mSongsList.size()) {
                        position = 0;
                    }
                    song = mSongsList.get(position);
                    song.setPlay(Key.PLAY);
                    song.setCountOfPlay(song.getCountOfPlay() + 1);
                    if (song.getCountOfPlay() == Key.IS_FAVORITE) {
                        song.setLike(Key.LIKE);
                        mFavoritesOperations.addSongFav(song);
                    }
                    // mINotification.onClickNotification(position);
                    mAllSongOperations.updateSong(song);
                    playMedia(song);
                    sendNotification(getBaseContext(), song, position);

                    mPosition = position;
                    break;
                }
                case Key.ACTION_PLAY_SONG:{
                    if (isPlaying()) {
                        mSongsList.get(mPosition).setPlay(Key.PAUSE);
                        pause();
                    } else {
                        mSongsList.get(mPosition).setPlay(Key.PLAY);
                        play();
                    }
                    mAllSongOperations.updateSong(mSongsList.get(mPosition));
                    sendNotification(getBaseContext(), mSongsList.get(mPosition), mPosition);
                    break;
                }
                case Key.ACTION_PREVIOUS_SONG:{
                    mSongsList.get(mPosition).setPlay(Key.STOP);
                    mAllSongOperations.updateSong(mSongsList.get(mPosition));

                    int position = mPosition - 1;
                    if (position == -1) {
                        position = mSongsList.size()-1;
                    }

                    song = mSongsList.get(position);
                    song.setPlay(Key.PLAY);
                    song.setCountOfPlay(song.getCountOfPlay() + 1);
                    if (song.getCountOfPlay() == Key.IS_FAVORITE) {
                        song.setLike(Key.LIKE);
                        mFavoritesOperations.addSongFav(song);
                    }
                    mAllSongOperations.updateSong(song);
                    playMedia(mSongsList.get(position));
                    sendNotification(getBaseContext(), song, position);

                    mPosition = position;
                    break;
                }
            }
        }

        return START_STICKY;
    }

    public void sendNotification(Context context, Song song, int position) {
        if (mINotification != null) {
            mINotification.onClickNotification(position);
        }
        switch (song.isLike()) {
            case Key.NO_LIKE:{
                mImgLike = R.drawable.ic_like;
                mImgDislike = R.drawable.ic_dislike;
                break;
            }
            case Key.LIKE:{
                mImgLike = R.drawable.ic_like_black;
                mImgDislike = R.drawable.ic_dislike;
                break;
            }
            case Key.DISLIKE:{
                mImgLike = R.drawable.ic_like;
                mImgDislike = R.drawable.ic_dislike_black;
                break;
            }
            default:
                throw new IllegalStateException("Unexpected value: " + song.isLike());
        }

        Intent intentNextSong = new Intent(context, MusicService.class)
                .setAction(Key.ACTION_NEXT_SONG).putExtra(Key.KEY_POSITION, position);
        PendingIntent pendingIntentNext =
                PendingIntent.getService(context, REQUEST_CODE, intentNextSong, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentPlaySong = new Intent(context, MusicService.class)
                .setAction(Key.ACTION_PLAY_SONG).putExtra(Key.KEY_POSITION, position);

        PendingIntent pendingIntentPlay =
                PendingIntent.getService(context, REQUEST_CODE, intentPlaySong, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentPreSong = new Intent(context, MusicService.class)
                .setAction(Key.ACTION_PREVIOUS_SONG).putExtra(Key.KEY_POSITION, position);
        PendingIntent pendingIntentPre =
                PendingIntent.getService(context, REQUEST_CODE, intentPreSong, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentLikeSong = new Intent(context, MusicService.class)
                .setAction(Key.ACTION_LIKE_SONG).putExtra(Key.KEY_POSITION, position);
        PendingIntent pendingIntentLike =
                PendingIntent.getService(context, REQUEST_CODE, intentLikeSong, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentDislikeSong = new Intent(context, MusicService.class)
                .setAction(Key.ACTION_DISLIKE_SONG).putExtra(Key.KEY_POSITION, position);
        PendingIntent pendingIntentDislike =
                PendingIntent.getService(context, REQUEST_CODE, intentDislikeSong, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentActivity = new Intent(context, MainActivity.class)
                .putExtra(Key.KEY_POSITION, position)
                .putExtra("Notification", true);
        PendingIntent pendingIntentActivity =
                PendingIntent.getActivity(context, REQUEST_CODE, intentActivity, PendingIntent.FLAG_UPDATE_CURRENT);

        RemoteViews notification_small = new RemoteViews(context.getPackageName(), R.layout.notification_small);
        RemoteViews notification_big = new RemoteViews(context.getPackageName(), R.layout.notification_big);

        long image = song.getImage();
        Bitmap bitmap = null;
        try {
            if (context.getContentResolver() != null) {
                bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), song.queryAlbumUri(image));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_notification);
        }

        notification_small.setImageViewBitmap(R.id.image_music_notification, bitmap);

        notification_big.setTextViewText(R.id.tv_song_name_notification, song.getTitle());
        notification_big.setTextViewText(R.id.tv_song_author_notification, song.getSubTitle());
        notification_big.setImageViewBitmap(R.id.image_music_notification, bitmap);

        notification_small.setOnClickPendingIntent(R.id.icon_next_notification, pendingIntentNext);
        notification_small.setOnClickPendingIntent(R.id.icon_previous_notification, pendingIntentPre);
        notification_small.setOnClickPendingIntent(R.id.icon_play__notification_small, pendingIntentPlay);

        notification_big.setOnClickPendingIntent(R.id.icon_next_notification, pendingIntentNext);
        notification_big.setOnClickPendingIntent(R.id.icon_previous_notification, pendingIntentPre);
        notification_big.setOnClickPendingIntent(R.id.icon_play__notification_small, pendingIntentPlay);

        if (!mediaPlayer.isPlaying()) {
            notification_small.setImageViewResource(R.id.icon_play__notification_small, R.drawable.ic_play_notification);
            notification_small.setOnClickPendingIntent(R.id.icon_play__notification_small, pendingIntentPlay);
            notification_big.setImageViewResource(R.id.icon_play_notification_big, R.drawable.ic_play_notification);
            notification_big.setOnClickPendingIntent(R.id.icon_play_notification_big, pendingIntentPlay);
        } else {
            notification_small.setImageViewResource(R.id.icon_play__notification_small, R.drawable.ic_pause_notificaton);
            notification_small.setOnClickPendingIntent(R.id.icon_play__notification_small, pendingIntentPlay);
            notification_big.setImageViewResource(R.id.icon_play_notification_big, R.drawable.ic_pause_notificaton);
            notification_big.setOnClickPendingIntent(R.id.icon_play_notification_big, pendingIntentPlay);
        }

        Notification notificationMusic = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.splash_play_music_192)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setContentIntent(pendingIntentActivity)
                .setCustomContentView(notification_small)
                .setCustomBigContentView(notification_big)
                .build();

        mPosition = position;

        if (mIsNotification) {
            startForeground(NOTIFY_ID, notificationMusic);
            mIsNotification = false;
        } else {
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(NOTIFY_ID, notificationMusic);
        }
    }


    public void setMediaPlayer(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
    }


    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    
    // tiep tuc choi nhac
    public void play(){
        checkOnCompletionListener = false;
        mediaPlayer.start();
    }

    // dung choi nhac
    public void pause(){
        checkOnCompletionListener = false;
        mediaPlayer.pause();
    }

    // kiem tra xem nhac co dang phat ko
    public boolean isPlaying() {
        if(mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                return true;
            }
        }
        return false;
    }

    // replay bai hat
    public void looping(boolean isLoop) {
        mediaPlayer.setLooping(isLoop);
    }

    // chuyen den bai tiep theo
    public int nextSong() {
        mPosition++;
        if (mPosition == mSongsList.size()) {
            mPosition = 0;
        }
        return mPosition;
    }

    public int previousSong() {
        mPosition--;
        if (mPosition == 0) {
            mPosition = mSongsList.size()-1;
        }
        return mPosition;
    }

    // set onCompletionListener
    public void completeMusic(){
        mediaPlayer.setOnCompletionListener(mp -> {
            nextSong();
            playMedia(mSongsList.get(mPosition));
            sendNotification(getBaseContext(), mSongsList.get(mPosition), mPosition);
        });
    }

    // phat nhac
    public void playMedia(Song song){
        try {
            mediaPlayer.reset();
            checkOnCompletionListener = false;
            mediaPlayer.setDataSource(song.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // random song
    public void playRandomSong() {
        mediaPlayer.setOnCompletionListener(mp -> {
            mPosition = randomSong(mPosition);
            playMedia(mSongsList.get(mPosition));
            sendNotification(getBaseContext(), mSongsList.get(mPosition), mPosition);
        });
    }

    // ngau nhien bai hat tiep theo
    private int randomSong(int position) {
        Random random = new Random();
        int rdPos = random.nextInt(mSongsList.size());
        while ( position == rdPos ) {
            rdPos = random.nextInt(mSongsList.size());
        }
        return rdPos;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
        stopForeground(true);
    }


}