package com.example.music.fragment;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.music.Key;
import com.example.music.model.Song;

@SuppressWarnings("ALL")
public class FavSongFragment extends BaseSongFragment{
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        setHasOptionsMenu(false);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }


    public FavSongFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        new ItemTouchHelper(mSimpleCallback).attachToRecyclerView(mRecyclerView);
    }

    // them su kien vuot sang trai de xoa
    ItemTouchHelper.SimpleCallback mSimpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull  RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            removeLike(mSongList.get(viewHolder.getAdapterPosition()).getTitle());
            mFavoritesOperations.removeSong(mSongList.get(viewHolder.getAdapterPosition()).getTitle());
            //mFavSong.remove(viewHolder.getAdapterPosition());
            mSongList.remove(viewHolder.getAdapterPosition());
            mSongAdapter.notifyDataSetChanged();
        }
    };

    // xoa khoi bai hat yeu thich
    private void removeLike(String title) {
        for (Song song : mSongList) {
            if (song.getTitle().equals(title)) {
                song.setLike(Key.NO_LIKE);
                song.setCountOfPlay(0);
                mAllSongOperations.updateSong(song);
                break;
            }
        }
    }
}
