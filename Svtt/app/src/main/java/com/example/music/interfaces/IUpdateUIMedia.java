package com.example.music.interfaces;

import com.example.music.model.Song;
import com.example.music.service.MusicService;

import java.util.ArrayList;

public interface IUpdateUIMedia {
    void getPositionFromMainToMedia(ArrayList<Song> songList, int position);
    void getService(MusicService musicService);
}
