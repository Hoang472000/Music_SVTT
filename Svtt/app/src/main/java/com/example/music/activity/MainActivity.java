package com.example.music.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.example.music.Key;
import com.example.music.R;
import com.example.music.adapter.SongAdapter;
import com.example.music.broadcast.MusicReceiver;
import com.example.music.database.AllSongOperations;
import com.example.music.database.FavoritesOperations;
import com.example.music.fragment.AllSongFragment;
import com.example.music.fragment.FavSongFragment;
import com.example.music.fragment.MediaPlaybackFragment;
import com.example.music.interfaces.IAllSong;
import com.example.music.interfaces.IMediaSong;
import com.example.music.interfaces.INotification;
import com.example.music.interfaces.IUpdateUIAllSong;
import com.example.music.interfaces.IUpdateUIMedia;
import com.example.music.model.Song;
import com.example.music.service.MusicService;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

@SuppressWarnings("ALL")
public class MainActivity extends AppCompatActivity implements
        View.OnClickListener
        , IAllSong
        , IMediaSong
        , INotification {

    public static final String TAG = "MinhMX";
    private Menu mMenu;
    private TextView mTitle;
    private TextView mSubtitle;
    private ImageView mPlayPauseSong;
    private ImageView mImgSong;

    public LinearLayout mPlayerSheetAll;

    private DrawerLayout mDrawerLayout;


    private ArrayList<Song> mSongsList;
    private RecyclerView mRecyclerView;
    private int mCurrentPosition = -1;
    private String mSearchText = "";
    private Song mCurrentSong;

    private SongAdapter mSongAdapter;

    public ArrayList<Song> dataSongList = new ArrayList<>();

    private boolean mPlayContinueFlag = true;

    private boolean mCkeckPlay = true;                  // kiem tra xem activity dang hien thi fragment nao

    private boolean mCheckScreen = false;               // kiem tra che do man hinh ( ngang hay doc )

    private boolean mCheckPlayerSheet = false;          // kiem tra player sheet

    private boolean mCheckPlayMusic = false;
    private boolean mCheckBackPress = false;            // kiem tra backpressed
    private boolean mCheckAcitvity = true;              // kiem tra trang thai cua activity ( onPause hay onResume )

    private boolean mIsRepeat;

    private boolean mIsShuffle;

    private boolean mCheckAttach = false;
    private boolean mIsCheckMedia = false;
    private boolean mCheckFragmentFav = false;          // kiem tra dang o allsongfragment hay favsongfragment
    private boolean mCheckStartActivity = false;        // check start activity o notification

    private boolean mCheckMediaFragment = false;
    private int mCheckFavCount = 0;
    private Toolbar mToolbar;

    private final int MY_PERMISSION_REQUEST = 100;
    private int allSongLength;

    private FavoritesOperations mFavoritesOperations;   // favorite songs
    private AllSongOperations allSongOperations;       // all songs

    private Intent mupdateNotification;                      // intent service

    private MusicReceiver mReceiver = new MusicReceiver();

    private boolean mIsBinder = false;
    public MusicService musicService;          // service choi nhac

    private Display mDisplay;
    private Intent mIntnetBroadcast;

    private SharedPreferences mPreferences;

    public AllSongFragment mAllSongFragment = new AllSongFragment();
    private MediaPlaybackFragment mMediaPlaybackFragment = new MediaPlaybackFragment();
    private FavSongFragment mFavSongFragment = new FavSongFragment();

    public IUpdateUIAllSong mIUpdateUIAllSong;
    private IUpdateUIMedia mIUpdateUIMedia;

    private NavigationView mNavigationView;

    // ket noi den service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getMusicService();
            mIsBinder = true;
            displayFragment();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            mIsBinder = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
    }

    public MusicService getMusicService(){
        return musicService;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDisplay = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        // kiem tra xem dien thoai dang o trang thai nao ( portrait hay landscape)
        if (mDisplay.getRotation() == Surface.ROTATION_90
                || mDisplay.getRotation() == Surface.ROTATION_270) {
            setContentView(R.layout.activity_main_landscape);
            mCheckScreen = true;

        } else {
            setContentView(R.layout.activity_main);
            mCheckScreen = false;

        }
        mDrawerLayout = findViewById(R.id.drawer_layout);
        mPlayerSheetAll = findViewById(R.id.linear_play_sheet_all);

        grantedPermission();
        broadcast();
        getIntentService();

        mFavoritesOperations = new FavoritesOperations(this);
        allSongOperations = new AllSongOperations(this);

        mSongsList = allSongOperations.getAllSong();

        if (mSongsList.size()>0) {
            allSongOperations.updatePlaySong(mSongsList);
            dataSongList.addAll(mSongsList);
        } else {
            mPlayerSheetAll.setVisibility(View.GONE);
        }

        init();

        mPreferences = getSharedPreferences(Key.SHARE_PREFERENCES, MODE_PRIVATE);

        if (mPreferences != null) {
            mIsRepeat = mPreferences.getBoolean(Key.REPEAT, false);
            mIsShuffle = mPreferences.getBoolean(Key.SHUFFLE, false);
            mCurrentPosition = mPreferences.getInt(Key.KEY_POSITION, 0);
        }

        Intent intent = this.getIntent();
        if (intent != null) {
            mCheckStartActivity = intent.getBooleanExtra("Notification", false);
        }

        // cap nhat lai trang thai cua UI sau khi xoay man hinh
        if (mSongsList.size() > 0) {
            if (savedInstanceState != null) {
                mCurrentPosition = savedInstanceState.getInt(Key.KEY_POSITION);
                mSongsList.get(mCurrentPosition).setPlay(Key.PAUSE);
                allSongOperations.updateSong(mSongsList.get(mCurrentPosition));
                attachMusic(mSongsList.get(mCurrentPosition));
                playSong(mCurrentPosition);
            }
            if (mCheckStartActivity) {
                mCurrentPosition = intent.getIntExtra(Key.KEY_POSITION, mCurrentPosition);
                playMusic(mSongsList.get(mCurrentPosition));
                mSongsList.get(mCurrentPosition).setPlay(Key.PLAY);
                allSongOperations.updateSong(mSongsList.get(mCurrentPosition));
            } else {
                playMusic(mSongsList.get(mCurrentPosition));
            }
        }
    }

    // cap nhat lai giao dien khi xoay man hinh
    private void playSong(int posistion) {
        Song song = mSongsList.get(posistion);
        song.setPlay(Key.PLAY);
        allSongOperations.updateSong(song);
        updateUI(posistion);
    }

    public void setIUpdateUI (IUpdateUIAllSong iUpdateUIAllSong) {
        this.mIUpdateUIAllSong = iUpdateUIAllSong;
    }

    public void setIUpdateUIMedia (IUpdateUIMedia iUpdateUIMedia) {
        this.mIUpdateUIMedia = iUpdateUIMedia;
    }

    public ArrayList<Song> getSongList() {
        return mSongsList;
    }

    // anh xa cac view
    @SuppressLint("NonConstantResourceId")
    private void init() {

        // cac view thuoc player sheet de hien thi bai hat dang choi o all song fragment
        mTitle = findViewById(R.id.tv_music_name);
        mSubtitle = findViewById(R.id.tv_music_subtitle);
        mPlayPauseSong = findViewById(R.id.play_pause_song);
        mImgSong = findViewById(R.id.iv_music_list);

        mNavigationView = findViewById(R.id.nav_view);

        mToolbar = findViewById(R.id.toolbar);

        mToolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(mToolbar);

        // action bar
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.menu_icon);

        mPlayPauseSong.setOnClickListener(this);
        mPlayerSheetAll.setOnClickListener(this);

        // navigation drawer
        mNavigationView.setNavigationItemSelectedListener(item -> {
            mDrawerLayout.closeDrawers();
            switch (item.getItemId()) {
                case R.id.listen_now:{
                    Toast.makeText(this, "Dang trien khai chuc nang nay=.=!", Toast.LENGTH_SHORT).show();
                    break;
                }

                // List favorite song
                case R.id.favorite: {
                    mCheckFavCount++;
                    replaceFavSongFragment();
                    break;
                }

                case R.id.recent:{
                    Toast.makeText(this, "Dang trien khai chuc nang nay=.=!", Toast.LENGTH_SHORT).show();
                    break;
                }

                // List song
                case R.id.library:{
                    mSongsList.clear();
                    mSongsList.addAll(dataSongList);
                    replaceNewFragment();
                    break;
                }
                case R.id.setting: {
                    Toast.makeText(this, "Dang trien khai chuc nang nay=.=!", Toast.LENGTH_SHORT).show();
                    break;
                }
                case R.id.nav_about: {
                    about();
                    break;
                }

            }
            return true;
        });
    }

    private void displayFragment() {
        // xac dinh man hinh ngang hay doc de hien thi cac fragment
        if (mDisplay.getRotation() == Surface.ROTATION_90 ||
                mDisplay.getRotation() == Surface.ROTATION_270){
            mCheckScreen = true;
            mPlayerSheetAll.setVisibility(View.GONE);
            replaceAllSong();
            if (mCurrentPosition == -1) {
                mCurrentPosition = 0;
            }
            if (mSongsList.size()>0) {
                replaceMedia(mCurrentPosition);
                musicService.setINotification(this);
                musicService.sendNotification(this, mSongsList.get(mCurrentPosition), mCurrentPosition);
            }
        } else {
            mCheckScreen = false;
            if (mSongsList.size() > 0) {
                mPlayerSheetAll.setVisibility(View.VISIBLE);
            }
            replaceAllSong();
            if (musicService!=null) {
                if (musicService.getMediaPlayer().isPlaying()) {
                    mPlayPauseSong.setImageResource(R.drawable.ic_pause_black);
                    musicService.completeMusic();
                    mCheckPlayMusic = true;
                } else {
                    mPlayPauseSong.setImageResource(R.drawable.ic_play_black);
                }
            }

            if (mSongsList.size() >0) {
                musicService.setINotification(this);
                musicService.sendNotification(this, mSongsList.get(mCurrentPosition), mCurrentPosition);
//                mIUpdateUIAllSong.getPositionFromMain(mSongsList, mCurrentPosition);
            }

            FragmentManager fragmentManager = getSupportFragmentManager();

            MediaPlaybackFragment mediaPlaybackFragment =
                    (MediaPlaybackFragment) fragmentManager.findFragmentById(R.id.fragment_media);
            if (mediaPlaybackFragment != null) {
                FragmentTransaction fragmentTransaction =
                        fragmentManager.beginTransaction();

                fragmentTransaction.remove(mediaPlaybackFragment)
                        .commit();
            }
        }
    }

    // premission
    private void grantedPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST);
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST);
            } else {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    Snackbar snackbar = Snackbar.make(mDrawerLayout, "Provide the Storage Permission", Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
            }
        }
    }

    // request permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                    getData();
                    replaceNewFragment();
                } else {
                    Snackbar snackbar = Snackbar.make(mDrawerLayout, "Provide the Storage Permission", Snackbar.LENGTH_LONG);
                    snackbar.show();
                    mPlayerSheetAll.setVisibility(View.GONE);
                    finish();
                }
            }
        }
    }


    // get data
    private void getData(){
        mSongsList = new ArrayList<>();
        getMusic(mSongsList);

        // save vao database
        for (int i=0; i<mSongsList.size(); i++) {
            allSongOperations.addAllSong(mSongsList.get(i));
            dataSongList.add(mSongsList.get(i));
        }
    }

    // lay nhac tu trong database cua may
    private void getMusic(ArrayList<Song> mDataSongList) {
        String selection = MediaStore.Audio.Media.IS_MUSIC + "=?";
        String orderBy = MediaStore.Audio.Media.TITLE + " ASC";
        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = getContentResolver().query(songUri, null, selection, new String[]{String.valueOf(1)}, orderBy);
        if (cursor != null && cursor.moveToFirst()) {
            int songTitle = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int songSubTitle = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int path = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            int duration = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int id = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            while (cursor.moveToNext()) {
                if (cursor.getString(duration) != null) {
                    mDataSongList.add(new Song(cursor.getString(songTitle)
                            , cursor.getString(songSubTitle)
                            , getTimeFormatted(Long.parseLong(cursor.getString(duration)))
                            , cursor.getString(path)
                            , cursor.getLong(id)
                            , 0
                            , Key.NO_LIKE
                            , Key.STOP));
                }
            }
        }
    }

    // dinh dang lai thoi gian
    private String getTimeFormatted(long milliSeconds) {
        String finalTimerString = "";
        String secondsString;

        int hours = (int) (milliSeconds / 3600000);
        int minutes = (int) (milliSeconds % 3600000) / 60000;
        int seconds = (int) ((milliSeconds % 3600000) % 60000 / 1000);

        if (hours > 0)
            finalTimerString = hours + ":";

        if (seconds < 10)
            secondsString = "0" + seconds;
        else
            secondsString = "" + seconds;

        finalTimerString = finalTimerString + minutes + ":" + secondsString;
        return finalTimerString;
    }

    // Hien song da choi o duoi thanh choi nhac
    private void playMusic(Song song) {
        if (song != null) {
            mTitle.setText(song.getTitle());
            mSubtitle.setText(song.getSubTitle());
            song.getImageAlbum(this, mImgSong, song.getImage());
            if (musicService != null) {
                if (musicService.isPlaying()) {
                    mPlayPauseSong.setImageResource(R.drawable.ic_pause_black);
                } else {
                    mPlayPauseSong.setImageResource(R.drawable.ic_play_black);
                }
            }
        }
    }

    // them action va dang ki broadcast
    private void broadcast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Key.ACTION_PLAY_NEXT_SONG);
        registerReceiver(mReceiver, intentFilter);
        // mReceiver.setINotification(this);
    }

    // hien thi thong tin app
    private void about() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.about))
                .setMessage(getString(R.string.about_text))
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // them su kien cho cac id o navigation drawer
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(Gravity.START);
                return true;
            case R.id.menu_search:
                Toast.makeText(this, "Search", Toast.LENGTH_SHORT).show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // su kien onclick cua cac button
    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            // layout choi nhac o ben all song fragment
            case R.id.linear_play_sheet_all:{
                if (!mCheckPlayMusic) {
                    mSongsList = dataSongList;
                    mCheckPlayMusic = true;
                    attachMusic(mSongsList.get(mCurrentPosition));
                }
                replaceFragment(mCurrentPosition);
                mCheckAttach = false;
                mPlayerSheetAll.setVisibility(View.GONE);
                mCkeckPlay = true;
                mCheckBackPress = false;
                mCheckPlayerSheet = true;
                break;
            }

            // play/pause song
            case R.id.play_pause_song:{
                if (!mCheckPlayMusic) {
                    attachMusic(dataSongList.get(mCurrentPosition));
                    mPlayPauseSong.setImageResource(R.drawable.ic_pause_black);
                    mSongsList = dataSongList;
                    mSongsList.get(mCurrentPosition).setPlay(Key.PLAY);
                    replaceAllSong();
                    mIUpdateUIAllSong.getPositionFromMain(mSongsList, mCurrentPosition);
                    musicService.setSonglist(mSongsList);
                    mCheckPlayMusic = true;
                } else {
                    if (musicService.isPlaying()) {
                        musicService.pause();
                        mSongsList.get(mCurrentPosition).setPlay(Key.PAUSE);
                        mPlayPauseSong.setImageResource(R.drawable.ic_play_black);
                    } else if (!musicService.isPlaying()) {
                        musicService.play();
                        mSongsList.get(mCurrentPosition).setPlay(Key.PLAY);
                        mPlayPauseSong.setImageResource(R.drawable.ic_pause_black);
                    }
                }
                updateNotification(mCurrentPosition);
                break;
            }
        }
    }

    // replace mediafragment
    private void replaceFragment(int position){
        if (mSongsList.size() == 0) {
            mSongsList.addAll(dataSongList);
        }
        if (mCheckFragmentFav) {
            mCheckFavCount++;
        }
        mCheckPlayerSheet = true;
        mCheckMediaFragment = true;
        mMediaPlaybackFragment.setArguments(getBundle(mSongsList.get(position), position));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment, mMediaPlaybackFragment)
                .addToBackStack("fragment")
                .commit();
    }

    // replace new all song fragment
    private void replaceNewFragment(){
        mCheckFragmentFav = false;
        mAllSongFragment = new AllSongFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment, mAllSongFragment)
                .addToBackStack("fragment")
                .commit();
    }

    // phat nhac
    private void attachMusic(Song song){
        if (!mCheckScreen) {
            mToolbar.setTitle(song.getTitle());
            mToolbar.setTitleTextColor(Color.WHITE);
        }
        mCheckAttach = true;
        if (musicService != null) {
            //completeMusic();
            musicService.completeMusic();
            musicService.playMedia(song);
            musicService.looping(mIsRepeat);
        }
    }

    // hien thi bai hat tiep theo
    private void musicNextPre(ArrayList<Song> songList, int position){
        if (mCkeckPlay) {
            updateUI(position);
        }
        updateNotification(mCurrentPosition);
    }

    // tang luot choi nhac
    // neu luot choi lon hon 3 tu dong cho vao danh sach yeu thich
    private void setCountPlay(Song song) {
        song.setCountOfPlay(song.getCountOfPlay() + 1);
        song.setPlay(Key.PLAY);
        if (song.getCountOfPlay() >= Key.IS_FAVORITE) {
            song.setLike(Key.LIKE);
            if (mFavoritesOperations.checkFavorites(song.getTitle())){
                favMusic(song);
            }
        }
        allSongOperations.updateSong(song);
    }

    // back ve fragment dau tien
    @Override
    public void onBackPressed() {
        int count = getSupportFragmentManager().getBackStackEntryCount();
        if (mCheckFavCount < 2) {
            mCheckFragmentFav = false;
            mCheckFavCount = 0;
            if (mNavigationView.getCheckedItem() != null){
                mNavigationView.getCheckedItem().setChecked(false);
            }
        }

        if (!mCheckScreen) {
            if (!mCheckFragmentFav) {
                mCheckFavCount = 0;
                String title = mSongsList.get(mCurrentPosition).getTitle();
                mSongsList.clear();
                mSongsList = allSongOperations.getAllSong();
                for (int i = 0; i < dataSongList.size(); i++) {
                    if (dataSongList.get(i).getTitle().equals(title)) {
                        this.mCurrentPosition = i;
                        break;
                    }
                }

                allSongOperations.updatePlaySong(mSongsList);
                mSongsList.get(mCurrentPosition).setPlay(Key.PLAY);
                allSongOperations.updateSong(mSongsList.get(mCurrentPosition));
                replaceAllSong();

            } else {
                replaceFavSongFragment();
                mCheckFavCount--;
            }
            mIUpdateUIAllSong.getPositionFromMain(mSongsList, mCurrentPosition);
            mIsShuffle = mPreferences.getBoolean(Key.SHUFFLE, false);
            mIsRepeat = mPreferences.getBoolean(Key.REPEAT, false);
            if (musicService.isPlaying()) {
                if (mIsRepeat) {
                    musicService.looping(mIsRepeat);
                } else if (mIsShuffle) {
                    musicService.playRandomSong();
                } else {
                    musicService.completeMusic();
                }
            }

            mDrawerLayout.closeDrawers();
            musicService.setINotification(this);
            musicService.setSonglist(mSongsList);

            playMusic(mSongsList.get(mCurrentPosition));
            mToolbar.setTitle(mSongsList.get(mCurrentPosition).getTitle());
            mPlayerSheetAll.setVisibility(View.VISIBLE);

            if (musicService.isPlaying()) {
                mPlayPauseSong.setImageResource(R.drawable.ic_pause_black);
            } else {
                mPlayPauseSong.setImageResource(R.drawable.ic_play_black);
            }

            mCheckBackPress = true;
            mCheckPlayerSheet = false;
            mCkeckPlay = false;
            mCheckAttach = true;
        } else {
            finish();
        }
    }

    // gui du lieu qua bundle
    private Bundle getBundle(Song currSong, int position){
        Bundle bundle = new Bundle();
        bundle.putLong(Key.CONST_IMAGE, currSong.getImage());
        bundle.putInt(Key.CONST_LIKE, currSong.isLike());
        bundle.putString(Key.CONST_TITLE, currSong.getTitle());
        bundle.putString(Key.CONST_SUBTITLE, currSong.getSubTitle());
        bundle.putString(Key.PATH_SONG, currSong.getPath());
        bundle.putInt(Key.KEY_POSITION, mCurrentPosition);
        bundle.putBoolean(Key.FAVORITE_FRAGMENT, mCheckFragmentFav);
        return bundle;
    }

    // cap nhat lai notification
    private void updateNotification(int position) {
        Song song = null;
        if (mSongsList != null) {
            song = mSongsList.get(position);
        }
        if (musicService.isPlaying()) {
            song.setPlay(Key.PLAY);
        } else {
            song.setPlay(Key.PAUSE);
        }
        allSongOperations.updateSong(song);
        musicService.sendNotification(this, song, position);
    }

    // them bai hat vao danh sach yeu thich
    private void favMusic(Song favSong) {
        mFavoritesOperations.addSongFav(favSong);
    }

    // phat bai hat duoc gui tu allsongfragment
    @Override
    public void onDataPassSong(Song song, int position) {
        this.mCurrentPosition = position;
        attachMusic(song);
        mCkeckPlay = true;
        playMusic(song);

        //mCheckPlayerSheet = false;
        mCheckPlayMusic = true;

        musicService.setINotification(this);
        musicService.setSonglist(mSongsList);

        musicService.sendNotification(this, song, position);

        //kiem tra xem dt dang xoay theo chieu ngang hay doc de gui du lieu sang mediafragment
        if (mCheckScreen) {
            mPlayerSheetAll.setVisibility(View.GONE);
        } else {
            if (mCheckMediaFragment) {
                replaceFragment(mCurrentPosition);
                mPlayerSheetAll.setVisibility(View.GONE);
            } else {
                mPlayerSheetAll.setVisibility(View.VISIBLE);
            }
        }
    }

    private void getIntentService() {
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        if (musicService != null) {
            musicService.setINotification(this);
        }
    }

    @Override
    public void fullSongList(ArrayList<Song> songList) {
        this.mSongsList = songList;
        this.mPlayContinueFlag = true;
    }

    // phat bai hat tiep theo da duoc chon o ben allsongfragment
    @Override
    public void currentSong(Song song, int position) {
        this.mCurrentSong = song;
        this.mCurrentPosition = position;
        attachMusic(mCurrentSong);
        playMusic(mCurrentSong);
        setCountPlay(mCurrentSong);
        musicService.setINotification(this);
        musicService.setSonglist(mSongsList);
        musicService.sendNotification(this, song, position);
        if (mCheckScreen) {
            updateUIMedia(position);
        }
    }

    // gui interface sang mediafragment
    private void updateUIMedia(int position){
        mIsCheckMedia = true;
        mIUpdateUIMedia.getPositionFromMainToMedia(mSongsList, position);
        mIUpdateUIMedia.getService(musicService);
        replaceMedia(position);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mCheckAcitvity) {
            if (!mCheckPlayerSheet) {
                if (mSongsList == null) {
                    mSongsList.addAll(dataSongList);
                }
                if (mSongsList.size()>0) {
                    playMusic(mSongsList.get(mCurrentPosition));
                }
                if (!mCheckFragmentFav) {
                    replaceAllSong();
                } else {
                    replaceFavSongFragment();
                }
            }
        }
        mCheckAcitvity = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCheckAcitvity = false;
        // luu position cua bai hat dang phat vao preference khi activity o trang thai on pasue
        SharedPreferences.Editor preferencesEditor = mPreferences.edit();
        preferencesEditor.putInt(Key.KEY_POSITION, mCurrentPosition);
        preferencesEditor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        unbindService(serviceConnection);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    // luu trang thai khi xoay man hinh
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(Key.KEY_POSITION, mCurrentPosition);
    }

    // update lai ui khi cap nhat lai thong bao
    @Override
    public void onClickNotification(int position) {
        this.mCurrentPosition = position;
//        attachMusic(mSongsList.get(position));
//        musicNextPre(mSongsList, position);
        updateUI(position);
    }

    // cap nhat giao dien
    private void updateUI(int position) {
        // check xem activity co hoat dong ko
        if (mCheckAcitvity) {
            // check man hinh xoay ngang hay dung
            if (mCheckScreen) {
                if (mSongsList == null) {
                    mSongsList = dataSongList;
                }
                replaceMedia(position);
                if (mIUpdateUIAllSong != null) {
                    mIUpdateUIAllSong.getPositionFromMain(mSongsList, mCurrentPosition);
                }
                if (mIsCheckMedia) {
                    mIUpdateUIMedia.getPositionFromMainToMedia(mSongsList, mCurrentPosition);
                    mIUpdateUIMedia.getService(musicService);
                }
            } else {
                // check xem dang hien fragment nao
                if (!mCheckPlayerSheet) {
                    if (!mCheckFragmentFav) {
                        replaceAllSong();
                    }
                    mToolbar.setTitle(mSongsList.get(position).getTitle());
                    playMusic(mSongsList.get(position));
                } else {
                    replaceFragment(position);
                    mIUpdateUIMedia.getPositionFromMainToMedia(mSongsList, mCurrentPosition);
                }

                if (mIUpdateUIAllSong != null) {
                    mIUpdateUIAllSong.getPositionFromMain(mSongsList, mCurrentPosition);
                }
            }
        }
    }

    // replace all song
    private void replaceAllSong() {
        mCheckMediaFragment = false;
        mCheckFragmentFav = false;
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment, mAllSongFragment)
                .addToBackStack("fragment")
                .commit();
    }

    // replace favorite song fragment
    private void replaceFavSongFragment() {
        if (!mCheckFragmentFav) {
            mSongsList.clear();
            mSongsList = mFavoritesOperations.getAllFavorites();
            mFavoritesOperations.setPlayMusic(mSongsList);
        }
        mCheckMediaFragment = false;
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment, mFavSongFragment)
                .addToBackStack("fragment")
                .commit();
        this.mCheckFragmentFav = true;
    }

    // replace media fragment
    private void replaceMedia(int position) {
        mMediaPlaybackFragment = new MediaPlaybackFragment();
        if (mSongsList.size()>0) {
            mMediaPlaybackFragment.setArguments(getBundle(mSongsList.get(position), position));
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_media, mMediaPlaybackFragment)
                    .addToBackStack("fragment")
                    .commit();
        }
    }


    @Override
    public void getSongListMedia(ArrayList<Song> songList, int position) {
        musicService.setSonglist(songList);
        musicService.setINotification(this);
        musicService.sendNotification(this, songList.get(position), position);
    }

    @Override
    public void getPositionMedia(int position) {
        this.mCurrentPosition = position;
    }

    @Override
    public boolean checkScreenMedia() {
        return mCheckScreen;
    }
}