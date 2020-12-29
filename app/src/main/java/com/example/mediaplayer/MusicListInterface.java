package com.example.mediaplayer;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MusicListInterface {
    @GET("MusicList.php")
    Call<JsonObject> GetMusicList();

    @GET("MusicList.php")
    Call<JsonObject> GetMusicArt(
            @Query("filename") String filename
    );

    @GET("MusicList.php")
    Call<JsonObject> GetFullMusicArt(
            @Query("filename") String filename,
            @Query("fullsize") boolean fullsize
    );
}
