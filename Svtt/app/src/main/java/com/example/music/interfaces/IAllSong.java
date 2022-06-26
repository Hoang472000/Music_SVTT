package com.example.music.interfaces;

import com.example.music.model.Song;

import java.util.ArrayList;

public interface IAllSong {
    void onDataPassSong(Song song, int position);
    void fullSongList(ArrayList<Song> songList);
    void currentSong(Song song, int position);

}
