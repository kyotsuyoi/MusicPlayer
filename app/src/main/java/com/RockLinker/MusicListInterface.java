package com.RockLinker;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MusicListInterface {
    @GET("index.php")
    Call<JsonObject> GetMusicList(
            @Query("artist") String filename
    );

    @GET("index.php")
    Call<JsonObject> GetArtistList();

    @GET("index.php")
    Call<JsonObject> GetMusicArt(
            @Query("filename") String filename
    );

    @GET("index.php")
    Call<JsonObject> GetFullMusicArt(
            @Query("filename") String filename,
            @Query("fullsize") boolean fullsize
    );
}
