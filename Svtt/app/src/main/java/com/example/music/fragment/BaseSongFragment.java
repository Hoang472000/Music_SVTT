package com.example.music.fragment;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.music.Key;
import com.example.music.R;
import com.example.music.activity.MainActivity;
import com.example.music.adapter.SongAdapter;
import com.example.music.broadcast.MusicReceiver;
import com.example.music.database.AllSongOperations;
import com.example.music.database.FavoritesOperations;
import com.example.music.interfaces.IAllSong;
import com.example.music.interfaces.ICallBack;
import com.example.music.interfaces.IUpdateUIAllSong;
import com.example.music.model.Song;
import com.example.music.service.MusicService;

import java.util.ArrayList;

public class BaseSongFragment extends Fragment implements
        IUpdateUIAllSong
        , ICallBack {

    protected RecyclerView mRecyclerView;

    protected MainActivity mMainActivity;

    protected SongAdapter mSongAdapter;

    protected ArrayList<Song> mSongList;
    protected ArrayList<Song> mSearchList;
    protected ArrayList<Song> mSongListBeforeSearch = new ArrayList<>();

    protected AllSongOperations mAllSongOperations;
    protected FavoritesOperations mFavoritesOperations;
    protected int mCurrentPosition;

    protected MusicService mMusicService;
    protected IAllSong mIAllSong;
    private boolean mCheckPlay;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mMainActivity = (MainActivity) context;
        mAllSongOperations = new AllSongOperations(context);
        mFavoritesOperations = new FavoritesOperations(context);
        mIAllSong = (IAllSong) context;
        mMainActivity.setIUpdateUI(this);
    }

    public MusicService getMusicService() {
        return mMainActivity.getMusicService();
    }

    protected ArrayList<Song> getSongList() {
        return mMainActivity.getSongList();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mCheckPlay = false;
        mSongListBeforeSearch = mMainActivity.dataSongList;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_base_song, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = view.findViewById(R.id.recycler_all_song);
        mMusicService = getMusicService();
        if (mMusicService != null) {
            mMainActivity.setIUpdateUI(this);
            if (mMusicService.isPlaying()) {
                mCheckPlay = true;
            }
        }
        setContent();
    }

    private void setContent() {
        mSongList = new ArrayList<>();
        mSearchList = new ArrayList<>();

        mSongList = getSongList();

        mSongAdapter = new SongAdapter(mMainActivity, mSongList, this);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mMainActivity));
        mRecyclerView.setAdapter(mSongAdapter);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.action_bar_menu, menu);

        SearchManager searchManager = (SearchManager) mMainActivity.getSystemService(Context.SEARCH_SERVICE);
        MenuItem menuItem = menu.findItem(R.id.menu_search);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setQueryHint("Search...");
        searchView.setSearchableInfo(searchManager.getSearchableInfo(mMainActivity.getComponentName()));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.equals("")){
                    mSongList.clear();
                    mSongList.addAll(mSongListBeforeSearch);
                    mSongAdapter.notifyDataSetChanged();
                } else {
                    onQueryTextChangeAdapter(newText);
                }
                return true;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    // so sanh voi text tim kiem va tra ve danh sach tuong ung
    @SuppressLint("NotifyDataSetChanged")
    private void onQueryTextChangeAdapter(String newText) {
        mSearchList.clear();
        String text = newText.toLowerCase();
        for (Song song : mSongListBeforeSearch) {
            String title = song.getTitle().toLowerCase();
            String subTitle = song.getSubTitle().toLowerCase();
            if (title.contains(text) || subTitle.contains(text)) {
                mSearchList.add(song);
            }
        }
        mSongList.clear();
        mSongList.addAll(mSearchList);
        mSongAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        setHasOptionsMenu(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setHasOptionsMenu(false);
    }

    // cap nhat lai giao dien all song
    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void getPositionFromMain(ArrayList<Song> songList, int position) {
        this.mSongList = songList;
        this.mCurrentPosition = position;
        mCheckPlay = true;
        mAllSongOperations.updatePlaySong(mSongList);
        Song song = mSongList.get(position);
        setPlayMusic(song);
        if (mSongAdapter != null) {
            mSongAdapter.notifyDataSetChanged();
        }
    }

    // set play va update database
    private void setPlayMusic(Song song){
        if (mMusicService != null) {
            if (mMusicService.isPlaying()){
                song.setPlay(Key.PLAY);
            } else {
                song.setPlay(Key.PAUSE);
            }
        } else {
            song.setPlay(Key.STOP);
        }
        mAllSongOperations.updateSong(song);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onResume() {
        super.onResume();
        mRecyclerView.scrollToPosition(mCurrentPosition);
        setHasOptionsMenu(true);
        mMainActivity.setIUpdateUI(this);
        if (mSongAdapter != null) {
            mSongAdapter.notifyDataSetChanged();
        }
    }

    // sau khi bam se, gui du lieu cho activity de phat bai hat do
    @Override
    public void onClickItem(int position) {
        mCheckPlay = true;
        mMusicService = getMusicService();
        mCurrentPosition = position;
        mAllSongOperations.updatePlaySong(mSongListBeforeSearch);
        setCountPlay(mSongList.get(position));
        mIAllSong.onDataPassSong(mSongList.get(position), position);
        mIAllSong.fullSongList(mSongList);
    }

    // tang luot choi nhac va neu bang 3 thi se tu them vao danh sach yeu thich
    @SuppressLint("NotifyDataSetChanged")
    private void setCountPlay(Song song) {
        song.setCountOfPlay(song.getCountOfPlay() + 1);
        if (song.getCountOfPlay() == Key.IS_FAVORITE) {
            song.setLike(Key.LIKE);
            if (mFavoritesOperations.checkFavorites(song.getTitle())) {
                mFavoritesOperations.addSongFav(song);
            }
        }
        song.setPlay(Key.PLAY);

        mAllSongOperations.updateSong(song);
        mSongAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLongClickItem(int position) {
        mMusicService = getMusicService();
        showDialog(position);
    }

    // hien thi dialog hoi xem co chon bai nay la bai hat tiep theo duoc phat
    @SuppressLint("NotifyDataSetChanged")
    private void showDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity);
        builder.setMessage(getString(R.string.play_next))
                .setCancelable(true)
                .setNegativeButton(R.string.no, (dialog, which) -> {
                })
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    if (mMusicService  != null) {
                        if (!mCheckPlay) {
                            mIAllSong.currentSong(mSongList.get(position), position);
                            setPlayMusic(mSongList.get(position));
                            mSongAdapter.notifyDataSetChanged();
                            mMusicService.sendNotification(mMainActivity, mSongList.get(position), position);
                            mCheckPlay = true;
                        } else {
                            mMusicService.getMediaPlayer().setOnCompletionListener(mp -> {
                                mCheckPlay = true;
                                mAllSongOperations.updatePlaySong(mSongList);

                                mCurrentPosition = position;

                                Song song = mSongList.get(mCurrentPosition);

                                Intent intent = new Intent(mMainActivity, MusicReceiver.class);
                                intent.setAction(Key.ACTION_PLAY_NEXT_SONG);
                                intent.putExtra(Key.CONST_TITLE, song.getTitle());
                                intent.putExtra(Key.CONST_SUBTITLE, song.getSubTitle());
                                intent.putExtra(Key.CONST_IMAGE, song.getImage());

                                mMainActivity.sendBroadcast(intent);

                                setPlayMusic(song);

                                mSongAdapter.notifyDataSetChanged();

                                mIAllSong.currentSong(song, mCurrentPosition);
                            });
                        }
                    } else {
                        Intent intent = new Intent(mMainActivity, MusicReceiver.class);
                        intent.setAction(Key.ACTION_LONG_CLICK);
                        mMainActivity.sendBroadcast(intent);
                    }
                    // setContent();
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

}