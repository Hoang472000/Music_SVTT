package com.example.music.interfaces;

import com.example.music.model.Song;

import java.util.ArrayList;

public interface IMediaSong {
    void getSongListMedia(ArrayList<Song> songList, int position);
    void getPositionMedia(int position);
    boolean checkScreenMedia();
}
