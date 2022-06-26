package com.example.music.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.ListFragment;

import com.example.music.Key;
import com.example.music.R;
import com.example.music.activity.MainActivity;
import com.example.music.database.AllSongOperations;
import com.example.music.database.FavoritesOperations;
import com.example.music.interfaces.IMediaSong;
import com.example.music.interfaces.IUpdateUIMedia;
import com.example.music.model.Song;
import com.example.music.service.MusicService;

import java.util.ArrayList;
import java.util.Random;

@SuppressWarnings("ALL")
public class MediaPlaybackFragment extends ListFragment implements
        PopupMenu.OnMenuItemClickListener
        , View.OnClickListener
        , IUpdateUIMedia {

    private ArrayList<Song> mSongsList = new ArrayList<>();

    private MainActivity mMainActivity;

    private int mCurrentPosition;

    private AllSongOperations mAllSongOperations;

    // cac view hien thi thong tin bai hat
    private ImageView mImageMusic;
    private ImageView mSmallImageMusic;
    private ImageView mLibaryMusic;
    private ImageView mMenuPopup;
    private TextView mTitle;
    private TextView mSubTitle;
    private TextView mTvPosition;

    // cac nut dieu khien choi nhac
    private ImageView mBtnPlayPause;
    private ImageButton mBtnPrev;
    private ImageButton mBtnNext;
    private ImageButton mBtnDisLike;
    private ImageButton mBtnLike;

    private ImageView mBtnShuffle;
    private ImageView mBtnReplay;

    private FrameLayout mLibraryLayout;

    private SeekBar mSeekbarController;
    private TextView mCurrentTime;
    private TextView mTotalTime;

    private IMediaSong mIMediaSong;

    private int mPosition;

    private boolean mIsCheck = false;
    private boolean mIsRepeat = false;                //  kiem tra che do lap lai
    private boolean mIsPlayContinue = true;
    private boolean mIsChekcLibrary = false;
    private boolean mIsLikeFlag = false;                  // kiem tra like cua bai hat
    private boolean mIsDislikeFlag = false;               // kiem tra dislike cua bai hat

    private boolean mIsShuffle = false;            // kiem tra che do ngau nhien bai hat
    private boolean mIsCheckPlay = false;
    private boolean mCheckFav;
    private FavoritesOperations mFavoritesOperations;
    private MusicService mMusicService;

    private BaseSongFragment mFragment;

    private Handler mHandler;
    private Runnable mRunnable;
    private SharedPreferences mPreferences;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        /* mMainActivity.setIUpdateUIMedia(this);*/

        // lay du lieu luu trong share preferences
        mPreferences = mMainActivity.getSharedPreferences(Key.SHARE_PREFERENCES, mMainActivity.MODE_PRIVATE);
        if (mPreferences != null) {
            mIsRepeat = mPreferences.getBoolean(Key.REPEAT, false);
            mIsShuffle = mPreferences.getBoolean(Key.SHUFFLE, false);
        }
    }

    protected MusicService getMusicService(){
        return mMainActivity.getMusicService();
    }

    public ArrayList<Song> getSongsList(){
        return mMainActivity.getSongList();
    }


    @Override
    public void onResume() {
        super.onResume();
        mMainActivity.setIUpdateUIMedia(this);
        Display display = ((WindowManager)getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        if (display.getRotation() == Surface.ROTATION_90 || display.getRotation() == Surface.ROTATION_270) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().show();
        } else {
            ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
        }
    }


    // ket noi den activity
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mIMediaSong = (IMediaSong) context;
        mFavoritesOperations = new FavoritesOperations(context);
        mAllSongOperations = new AllSongOperations(context);
        mMainActivity = (MainActivity) context;
        mMainActivity.setIUpdateUIMedia(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_media, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        /** anh xa cac view **/
        mImageMusic = view.findViewById(R.id.image_music);
        mSmallImageMusic = view.findViewById(R.id.iv_music_list);
        mLibaryMusic = view.findViewById(R.id.queue_music);
        mTitle = view.findViewById(R.id.tv_music_name_media);
        mSubTitle = view.findViewById(R.id.tv_music_subtitle);
        mTvPosition = view.findViewById(R.id.position);
        mMenuPopup = view.findViewById(R.id.more_vert);

        /** anh xa cac button choi nhac **/
        mBtnPlayPause = view.findViewById(R.id.img_btn_play);
        mBtnPrev = view.findViewById(R.id.img_btn_previous);
        mBtnNext = view.findViewById(R.id.img_btn_next);
        mBtnReplay = view.findViewById(R.id.img_btn_replay);
        mBtnDisLike = view.findViewById(R.id.img_btn_dislike);
        mBtnLike = view.findViewById(R.id.img_btn_like);
        mBtnShuffle = view.findViewById(R.id.img_btn_shuffle);


        mLibraryLayout = view.findViewById(R.id.library);

        mSeekbarController = view.findViewById(R.id.seekbar_controller);

        // textview hien thi thoi gian choi va tong thoi gian cua thanh seekbar
        mCurrentTime = view.findViewById(R.id.tv_current_time);
        mTotalTime = view.findViewById(R.id.tv_total_time);

        mSmallImageMusic.setVisibility(View.VISIBLE);
        mTvPosition.setVisibility(View.GONE);

        mMainActivity.mPlayerSheetAll.setVisibility(View.GONE);

        mMenuPopup.setOnClickListener(v -> {
            showMenuPopup(v);
        });

        mSongsList = getSongsList();                                            // lay songlist tu activity
        mMainActivity.setIUpdateUIMedia(this);                                  // set interface cho activity
        mMusicService = getMusicService();                                      // lay service tu activity

        // kiem tra trang thai man hinh de hien thi library listsong
        if (mIMediaSong.checkScreenMedia()) {
            mLibaryMusic.setVisibility(View.GONE);
        } else {
            mLibaryMusic.setVisibility(View.VISIBLE);
        }


        Bundle bundle = this.getArguments();

        // thong tin tuw bai hat dc gui tu all song
        if (bundle != null) {
            mCurrentPosition = bundle.getInt(Key.KEY_POSITION);
            Song song = mSongsList.get(mCurrentPosition);

            //displayMusic(song);
            mTitle.setText(bundle.getString(Key.CONST_TITLE));

            mSubTitle.setText(bundle.getString(Key.CONST_SUBTITLE));

            song.getImageAlbum(getContext(), mImageMusic, bundle.getLong(Key.CONST_IMAGE));
            song.getImageAlbum(getContext(), mSmallImageMusic, bundle.getLong(Key.CONST_IMAGE));

            mCheckFav = bundle.getBoolean(Key.FAVORITE_FRAGMENT);
            if (!mCheckFav) {
                for (int i = 0; i < mMainActivity.dataSongList.size(); i++) {
                    if (mMainActivity.dataSongList.get(i).getTitle().equals(song.getTitle())) {
                        this.mCurrentPosition = i;
                        break;
                    }
                }
            }
            int like = bundle.getInt(Key.CONST_LIKE);
            likeMuisc(like);
        }

        // kiem tra relay lai bai hat
        if (mIsRepeat) {
            mBtnReplay.setImageResource(R.drawable.ic_repeat_one);
            mMusicService.looping(mIsRepeat);
        }

        if (mMusicService != null) {
            setControls();
            if (mMusicService.isPlaying()) {
                // kiem tra xem dang phat o che do nao
                if (mIsShuffle) {
                    mBtnShuffle.setImageResource(R.drawable.ic_shuffle_black);
                    mMusicService.playRandomSong();
                } else {
                    mMusicService.completeMusic();
                }
            }
        } else {
            mTotalTime.setText(mSongsList.get(mCurrentPosition).getDuration());
        }

        mLibaryMusic.setOnClickListener(this);
        mBtnNext.setOnClickListener(this);
        mBtnPrev.setOnClickListener(this);
        mBtnReplay.setOnClickListener(this);
        mBtnPlayPause.setOnClickListener(this);
        mBtnLike.setOnClickListener(this);
        mBtnDisLike.setOnClickListener(this);
        mBtnShuffle.setOnClickListener(this);
    }

    // hien thong tin bai hat
    private void displayMusic(Song song){
        if (mTitle != null) {
            /*if (mIMediaSong.checkScreenMedia()) {
                mTitle.setSelected(false);
            } else {
                mTitle.setSelected(true);
            }*/
            mTitle.setText(song.getTitle());
            mSubTitle.setText(song.getSubTitle());
            song.getImageAlbum(getContext(), mImageMusic, song.getImage());
            song.getImageAlbum(getContext(), mSmallImageMusic, song.getImage());
            likeMuisc(song.isLike());
        }
    }

    // hien menu popup
    private void showMenuPopup(View view){
        PopupMenu popupMenu = new PopupMenu(getContext(), view);
        popupMenu.inflate(R.menu.menu_popup);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.show();
    }

    // su kien click o menupopup
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            // them bai hat vao playlist
            case R.id.favorite:{
                mSongsList.get(mCurrentPosition).setLike(1);
                mAllSongOperations.updateSong(mSongsList.get(mCurrentPosition));
                if (mFavoritesOperations.checkFavorites(mSongsList.get(mCurrentPosition).getTitle())) {
                    mFavoritesOperations.addSongFav(mSongsList.get(mCurrentPosition));
                }
                return true;
            }
            // xoa bai hat khoi playlist
            case R.id.delete:{
                mFavoritesOperations.removeSong(mSongsList.get(mCurrentPosition).getTitle());
                return true;
            }
        }
        return false;
    }

    @Override
    public void onStop() {
        super.onStop();
        ((AppCompatActivity)getActivity()).getSupportActionBar().show();
    }

    // bat su kien play/pause/next/previous
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            // mo list nhac
            case R.id.queue_music:{
                if (mIsChekcLibrary) {
                    mLibaryMusic.setImageResource(R.drawable.ic_queue_music);
                    mImageMusic.setVisibility(View.VISIBLE);
                    mBtnReplay.setVisibility(View.VISIBLE);
                    mBtnShuffle.setVisibility(View.VISIBLE);
                    mLibraryLayout.setVisibility(View.GONE);
                    mIsChekcLibrary = false;
                    removeFragment();
                } else {
                    mLibaryMusic.setImageResource(R.drawable.ic_queue_music_orange);
                    mImageMusic.setVisibility(View.GONE);
                    mBtnReplay.setVisibility(View.GONE);
                    mBtnShuffle.setVisibility(View.GONE);
                    mLibraryLayout.setVisibility(View.VISIBLE);
                    if (!mCheckFav) {
                        mFragment = new AllSongFragment();
                    } else {
                        mFragment = new FavSongFragment();
                    }

                    mMainActivity.getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.library, mFragment)
                            .commit();
                    mIsChekcLibrary = true;
                }
                break;
            }

            // play/pause ben mediafragment
            case R.id.img_btn_play: {
                if (mIsCheck) {
                    if (mMusicService.isPlaying()) {
                        mMusicService.pause();

                        updateCurrentSong(mCurrentPosition, Key.PAUSE);

                        mBtnPlayPause.setImageResource(R.drawable.ic_play_notification);
                        //mBtnPlayPause.setBackground(getContext().getDrawable(R.color.background));
                    } else if (!mMusicService.isPlaying()) {
                        mMusicService.play();

                        updateCurrentSong(mCurrentPosition, Key.PLAY);

                        mBtnPlayPause.setImageResource(R.drawable.ic_pause_notificaton);
                        playCycle();
                    }
                    updateNotification(mCurrentPosition);
                } else {
                    mMusicService = getMusicService();
                    mIMediaSong.getSongListMedia(mSongsList, mCurrentPosition);
                    playSong(mCurrentPosition);
                    mIsCheck = true;
                }
                break;
            }

            // replay lai bai hat dang dc phat
            case R.id.img_btn_replay: {
                if (mIsRepeat) {
                    Toast.makeText(getContext(), "Replaying Removed..", Toast.LENGTH_SHORT).show();
                    mBtnReplay.setImageResource(R.drawable.ic_repeat);
                    mMusicService.looping(false);
                    mIsRepeat = false;
                } else {
                    Toast.makeText(getContext(), "Replaying Added..", Toast.LENGTH_SHORT).show();
                    mBtnReplay.setImageResource(R.drawable.ic_repeat_one);
                    mMusicService.looping(true);
                    mIsRepeat = true;
                }
                break;
            }

            // quay lai bai hat truoc do
            case R.id.img_btn_previous: {
                updateRepeatShuffle();
                if (mIsCheck) {
                    // neu bai hat chay dc hon 3s thi phat lai tu dau
                    if (mSeekbarController.getProgress() > Key.IS_PREVIOUS) {
                        attachMusic(mSongsList.get(mCurrentPosition));
                    } else {
                        if (mCurrentPosition > 0) {
                            updateCurrentSong(mCurrentPosition, Key.STOP);

                            mCurrentPosition = mCurrentPosition - 1;
                            playSong(mCurrentPosition);

                            mIMediaSong.getPositionMedia(mCurrentPosition);

                        } else {
                            updateCurrentSong(mCurrentPosition, Key.STOP);

                            mCurrentPosition = mSongsList.size() - 1;
                            playSong(mCurrentPosition);

                            mIMediaSong.getPositionMedia(mCurrentPosition);
                        }
                    }
                } else {
                    // het bai hat
                    Toast.makeText(getContext(), "Select the Song ..", Toast.LENGTH_SHORT).show();
                }
                break;
            }

            // chuyen den bai hat tiep theo
            case R.id.img_btn_next: {
                updateRepeatShuffle();
                if (mIsCheck) {
                    if (mCurrentPosition + 1 < mSongsList.size()) {
                        updateCurrentSong(mCurrentPosition, Key.STOP);

                        mCurrentPosition += 1;
                        playSong(mCurrentPosition);

                        mIMediaSong.getPositionMedia(mCurrentPosition);
                    } else {
                        updateCurrentSong(mCurrentPosition, Key.STOP);

                        mCurrentPosition = 0;
                        playSong(mCurrentPosition);

                        mIMediaSong.getPositionMedia(mCurrentPosition);
                        mIsCheck = true;
                    }
                } else {
                    // het bai hat
                    Toast.makeText(getContext(), "Select the Song ..", Toast.LENGTH_SHORT).show();
                }
                break;
            }

            // phat ngau nhien bai hat trong list nhac
            case R.id.img_btn_shuffle:{
                if (mIsShuffle) {
                    mBtnShuffle.setImageResource(R.drawable.ic_shuffle);
                    mIsShuffle = false;
                    Toast.makeText(mMainActivity, "Remove Shuffe Song...", Toast.LENGTH_SHORT).show();
                } else {
                    mIsShuffle = true;
                    mBtnShuffle.setImageResource(R.drawable.ic_shuffle_black);
                    mMusicService.playRandomSong();
                }
                break;
            }

            // like bai hat va them vao danh sach ua thich
            case R.id.img_btn_like:{
                if (mIsLikeFlag) {
                    mIsLikeFlag = false;
                    mFavoritesOperations.removeSong(mSongsList.get(mCurrentPosition).getTitle());
                    mSongsList.get(mCurrentPosition).setLike(Key.NO_LIKE);

                    mSongsList.get(mCurrentPosition).setCountOfPlay(0);
                    mAllSongOperations.updateSong(mSongsList.get(mCurrentPosition));

                    mBtnLike.setImageResource(R.drawable.ic_like);

                } else {
                    mIsLikeFlag = true;
                    mIsDislikeFlag = false;

                    mSongsList.get(mCurrentPosition).setLike(Key.LIKE);
                    if (mFavoritesOperations.checkFavorites(mSongsList.get(mCurrentPosition).getTitle())){
                        favMusic(mSongsList.get(mCurrentPosition));
                    } else {
                        Toast.makeText(mMainActivity, "Bai hat da co trong playlist", Toast.LENGTH_SHORT).show();
                    }

                    mAllSongOperations.updateSong(mSongsList.get(mCurrentPosition));

                    mBtnLike.setImageResource(R.drawable.ic_like_black);
                    mBtnDisLike.setImageResource(R.drawable.ic_dislike);
                }
                break;
            }

            // dislike bai hat va xoa khoi danh sach yeu thich
            case R.id.img_btn_dislike: {
                if (mIsDislikeFlag) {
                    mIsDislikeFlag = false;
                    mSongsList.get(mCurrentPosition).setLike(Key.NO_LIKE);

                    mAllSongOperations.updateSong(mSongsList.get(mCurrentPosition));


                    mBtnDisLike.setImageResource(R.drawable.ic_dislike);
                } else {
                    mIsDislikeFlag = true;
                    mIsLikeFlag = false;

                    mFavoritesOperations.removeSong(mSongsList.get(mCurrentPosition).getTitle());
                    mSongsList.get(mCurrentPosition).setLike(Key.DISLIKE);
                    if (mSongsList.get(mCurrentPosition).getCountOfPlay() >= Key.IS_FAVORITE) {
                        mSongsList.get(mCurrentPosition).setCountOfPlay(0);
                    }
                    mAllSongOperations.updateSong(mSongsList.get(mCurrentPosition));
                    mBtnDisLike.setImageResource(R.drawable.ic_dislike_black);
                    mBtnLike.setImageResource(R.drawable.ic_like);
                }
                break;
            }
        }
    }

    private void removeFragment() {
        FragmentManager fragmentManager = mMainActivity.getSupportFragmentManager();
        if (!mCheckFav) {
            mFragment = (AllSongFragment) fragmentManager.findFragmentById(R.id.library);
        } else {
            mFragment = (FavSongFragment) fragmentManager.findFragmentById(R.id.library);
        }

        if (mFragment != null) {
            mFragment.onPause();
            fragmentManager.beginTransaction()
                    .remove(mFragment);
            mFragment = new BaseSongFragment();
        }
    }


    private void playSong(int position) {
        attachMusic(mSongsList.get(position));          // choi nha

        musicNextPre(mSongsList, position);             // cap nhat giao dien va thong bao

        setCountPlay(mSongsList.get(position));         // tang so lan choi nhac
    }

    // update lai gia tri play sau khi an next/previous or pasue/play
    private void updateCurrentSong(int position, int play){
        mSongsList.get(position).setPlay(play);
        mAllSongOperations.updateSong(mSongsList.get(position));
    }

    // update lai repeat va shuffe khi an next/previous
    private void updateRepeatShuffle() {
        mIsRepeat = false;
        mIsShuffle = false;
        mBtnReplay.setImageResource(R.drawable.ic_repeat);
        mBtnShuffle.setImageResource(R.drawable.ic_shuffle);
    }

    // phat nhac
    private void attachMusic(Song song){
        mBtnPlayPause.setImageResource(R.drawable.ic_pause_notificaton);
        mMusicService.playMedia(song);
        setControls();
        if (!mIsShuffle) {
            //completeMusic();
            mMusicService.completeMusic();
        }
    }

    // set thoi gian chay tren thanh seek bar
    private void setControls() {
        mSeekbarController.setMax(mMusicService.getMediaPlayer().getDuration());
        playCycle();
        mIsCheck = true;
        if (mMusicService.isPlaying()) {
            mBtnPlayPause.setImageResource(R.drawable.ic_pause_notificaton);
        }
        mTotalTime.setText(getTimeFormatted(mMusicService.getMediaPlayer().getDuration()));
        mSeekbarController.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mMusicService.getMediaPlayer().seekTo(progress);
                    mCurrentTime.setText(getTimeFormatted(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    // lay thoi chay
    private void playCycle() {
        try {
            mSeekbarController.setProgress(mMusicService.getMediaPlayer().getCurrentPosition());
            mCurrentTime.setText(getTimeFormatted(mMusicService.getMediaPlayer().getCurrentPosition()));
            if (mMusicService.isPlaying()) {
                mRunnable = () -> playCycle();;
                mHandler.postDelayed(mRunnable, 100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // dinh dang lai thoi gian
    private static String getTimeFormatted(long milliSeconds) {
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

    // next/previous bai hat
    private void musicNextPre(ArrayList<Song> songList, int position){
        //updateUI(position);
        updateNotification(mCurrentPosition);
    }

    // tang luot choi nhac
    private void setCountPlay(Song song) {
        song.setCountOfPlay(song.getCountOfPlay() + 1);
        song.setPlay(Key.PLAY);

        /** neu luot choi lon hon 3 tu dong cho vao danh sach yeu thich **/
        if (song.getCountOfPlay() >= Key.IS_FAVORITE) {
            song.setLike(Key.LIKE);
            mBtnLike.setImageResource(R.drawable.ic_like_black);
            if (mFavoritesOperations.checkFavorites(song.getTitle())){
                favMusic(song);
            }
        }
        mAllSongOperations.updateSong(song);
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

    // cap nhat lai notification
    private void updateNotification(int position) {
        Song song = null;
        if (mSongsList != null) {
            song = mSongsList.get(position);
        }

        // neu bai hat dang dc phat thi setPlay(1) va nguoc lai, cap nhat lai database
        if (mMusicService.isPlaying()) {
            song.setPlay(Key.PLAY);
        } else {
            song.setPlay(Key.STOP);
        }
        mAllSongOperations.updateSong(song);

        // cap nhat notification
        mMusicService.sendNotification(getContext(), song, position);
    }

    // them bai hat vao danh sach yeu thich
    private void favMusic(Song favSong) {
        mFavoritesOperations.addSongFav(favSong);
    }

    private void updateUI(int position) {
        displayMusic(mSongsList.get(position));
    }

    // hien thi nut like, dislike khi mo bai hat
    private void likeMuisc (int like){
        switch (like) {
            // bai hat
            case Key.NO_LIKE:{
                mIsLikeFlag = false;
                mIsDislikeFlag = false;
                mBtnLike.setImageResource(R.drawable.ic_like);
                mBtnDisLike.setImageResource(R.drawable.ic_dislike);
                break;
            }
            // bai hat ua thich
            case Key.LIKE:{
                mIsLikeFlag = true;
                mIsDislikeFlag = false;
                mBtnLike.setImageResource(R.drawable.ic_like_black);
                mBtnDisLike.setImageResource(R.drawable.ic_dislike);
                break;
            }
            // bai hat khong thich
            case Key.DISLIKE: {
                mIsDislikeFlag = true;
                mIsLikeFlag = false;
                mBtnLike.setImageResource(R.drawable.ic_like);
                mBtnDisLike.setImageResource(R.drawable.ic_dislike_black);
                break;
            }
        }
    }

    // khi mediafragment goi callback onPause thi se luu vi tri, mIsRepeat, mIsShuffe
    @Override
    public void onPause() {
        super.onPause();
        mLibraryLayout.setVisibility(View.GONE);
        mIsChekcLibrary = false;
        mLibaryMusic.setImageResource(R.drawable.ic_queue_music);
        removeFragment();
        SharedPreferences.Editor preferencesEditor = mPreferences.edit();
        preferencesEditor.putInt(Key.KEY_POSITION, mCurrentPosition);
        preferencesEditor.putBoolean(Key.REPEAT, mIsRepeat);
        preferencesEditor.putBoolean(Key.SHUFFLE, mIsShuffle);
        preferencesEditor.apply();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRunnable);
    }

    // cap nhat lai giao dien mediafragment
    @Override
    public void getPositionFromMainToMedia(ArrayList<Song> songList, int position) {
        this.mCurrentPosition = position;
        mIsCheck = true;
        this.mSongsList = songList;
        if (mMusicService == null) {
            mMusicService = getMusicService();
        }
        if (!mMusicService.isPlaying()) {
            mBtnPlayPause.setImageResource(R.drawable.ic_play_notification);
        } else {
            mBtnPlayPause.setImageResource(R.drawable.ic_pause_notificaton);
            playCycle();
        }
        setControls();
        displayMusic(mSongsList.get(position));
    }

    // truyen service tu activity sang
    @Override
    public void getService(MusicService musicService) {
        this.mMusicService = musicService;
    }
}

