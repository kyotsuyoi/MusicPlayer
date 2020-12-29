package com.example.mediaplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;

import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.mediaplayer.CommonClasses.ApiClient;
import com.example.mediaplayer.CommonClasses.OnClearFormRecentService;
import com.example.mediaplayer.CommonClasses.PlayerNotification;
import com.example.mediaplayer.CommonClasses.RecyclerItemClickListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.channels.SelectableChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String PREFERENCES = "MUSICPLAYER_PREFERENCES";

    private ImageView imageViewNext, imageViewPlay, imageViewPrevious;
    private ImageView imageViewArt;
    private static MediaPlayer mediaPlayer;

    private ImageView imageViewShuffle, imageViewShuffleCheck, imageViewRepeat, imageViewRepeatCheck,
            imageViewInternalMusicList, imageViewExternalMusicList, imageViewFavorite;
    private TextView textViewOneOrALL;

    private ProgressBar progress;

    private double currentTime = 0;
    private double finalTime = 0;

    private final Handler myHandler = new Handler();
    private SeekBar seekbar;
    private TextView textViewCurrentTime, textViewTotalTime, textViewMusicName, textViewArtistName;

    private SearchView searchView;

    private String path;

    private RecyclerView recyclerViewMusicList;
    private SwipeRefreshLayout swipeRefresh;
    private InternalMusicListAdapter internalAdapter;
    private ExternalMusicListAdapter externalAdapter;

    private final com.example.mediaplayer.CommonClasses.Handler Handler = new com.example.mediaplayer.CommonClasses.Handler();
    private final int R_ID = R.id.activityMain_ImageView_Play;
    private final MusicListInterface musicListInterface = ApiClient.getApiClient().create(MusicListInterface.class);

    public static String selectedFileName;
    private List<File> favoriteList;

    private NotificationManager notificationManager;

    public static JsonObject musicInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mediaPlayer = new MediaPlayer();

        imageViewNext = findViewById(R.id.activityMain_ImageView_Next);
        imageViewPlay = findViewById(R.id.activityMain_ImageView_Play);
        imageViewPrevious = findViewById(R.id.activityMain_ImageView_Previous);
        imageViewArt = findViewById(R.id.activityMain_ImageView_Art);

        textViewCurrentTime = findViewById(R.id.activityMain_TextView_CurrentTime);
        textViewTotalTime = findViewById(R.id.activityMain_TextView_TotalTime);
        textViewMusicName = findViewById(R.id.activityMain_TextView_MusicName);
        textViewArtistName = findViewById(R.id.activityMain_TextView_ArtistName);

        imageViewShuffle = findViewById(R.id.activityMain_ImageView_Shuffle);
        imageViewShuffleCheck = findViewById(R.id.activityMain_ImageView_ShuffleCheck);
        imageViewRepeat = findViewById(R.id.activityMain_ImageView_Repeat);
        imageViewRepeatCheck = findViewById(R.id.activityMain_ImageView_RepeatCheck);

        imageViewInternalMusicList = findViewById(R.id.activityMain_ImageView_InternalMusicList);
        imageViewExternalMusicList = findViewById(R.id.activityMain_ImageView_ExternalMusicList);

        textViewOneOrALL = findViewById(R.id.activityMain_TextView_RepeatOneOrAll);

        imageViewFavorite = findViewById(R.id.activityMain_ImageView_Favorite);

        progress = findViewById(R.id.activityMain_ImageView_ProgressBar);
        progress.setVisibility(View.INVISIBLE);

        imageViewShuffleCheck.setVisibility(View.INVISIBLE);
        imageViewRepeatCheck.setVisibility(View.INVISIBLE);

        textViewOneOrALL.setVisibility(View.INVISIBLE);

        seekbar = findViewById(R.id.activityMain_SeekBar);
        seekbar.setClickable(false);

        imageViewPlay.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.ic_play_arrow,getTheme()));
        //imageViewPlay.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow));

        path = Objects.requireNonNull(getExternalFilesDir(Environment.DIRECTORY_MUSIC)).getPath();

        favoriteList = new ArrayList<>();
        SetButtons();
        LoadPreferences();
        GetInternalMusicList();
        Shuffle();
        SetMusic(0);

        createChannel();

        TextView textViewVersion = findViewById(R.id.activityMain_TextView_Version);
        String ver = "Versão não encontrada";
        try {
            PackageInfo packageInfo;
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            ver = "Versão " + packageInfo.versionName;
            textViewVersion.setText(ver);
        } catch (Exception e) {
            textViewVersion.setText(ver);
        }

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean("isPaused", !mediaPlayer.isPlaying());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        //audioManager.unregisterMediaButtonEventReceiver(audioManagerReceiver);
        super.onDestroy();
        //unregisterReceiver(onDownloadComplete);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.cancelAll();
        }

        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_HEADSETHOOK){
            Play();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void createChannel(){

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    PlayerNotification.CHANNEL_ID,
                    "Test",
                    NotificationManager.IMPORTANCE_LOW
            );

            notificationManager = getSystemService(NotificationManager.class);
            if(notificationManager != null){
                notificationManager.createNotificationChannel(channel);
            }

            registerReceiver(broadcastReceiver, new IntentFilter("TRACKS_TRACK"));
            startService(new Intent(getBaseContext(), OnClearFormRecentService.class));

        }
    }

    private void SetButtons(){

        imageViewPlay.setOnClickListener(v -> Play());

        imageViewNext.setOnClickListener(v -> Next());

        imageViewPrevious.setOnClickListener(v -> Previous());

        imageViewRepeat.setOnClickListener(v -> {

            String OneOrAll = textViewOneOrALL.getText().toString();

            switch (OneOrAll){
                case "1":
                    textViewOneOrALL.setText("A");
                    break;
                case "A":
                    textViewOneOrALL.setText("N");
                    imageViewRepeatCheck.setVisibility(View.INVISIBLE);
                    textViewOneOrALL.setVisibility(View.INVISIBLE);
                    break;
                case "N":
                    textViewOneOrALL.setText("1");
                    imageViewRepeatCheck.setVisibility(View.VISIBLE);
                    textViewOneOrALL.setVisibility(View.VISIBLE);
                    break;
            }

            SavePreferences();
        });

        imageViewShuffle.setOnClickListener(v -> {
            //Handler.ShowSnack("Indisponível", "Função indisponível, pois causa problemas pra identificar qual musica está sendo executada na playlist", this, R_ID);
            Shuffle();
            SavePreferences();
        });

        imageViewInternalMusicList.setOnClickListener(v -> DialogInternalMusicList());

        imageViewExternalMusicList.setOnClickListener(v -> {
            GetInternalMusicList();
            GetExternalMusicList();
        });

        imageViewFavorite.setOnClickListener(v -> setFavorite());

        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentTime = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mediaPlayer.seekTo((int)currentTime);
            }
        });
    }

    private void Play(){
        try {
            /*if(internalAdapter.getItemCount() < 1){
                return;
            }*/

            if(mediaPlayer.getDuration() == 0) return;

            mediaPlayer.seekTo((int) currentTime);
            if (mediaPlayer.isPlaying()) {
                SetPlay(false);
                return;
            }

            SetPlay(true);
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.Play: " + e.getMessage(), MainActivity.this, R_ID);
        }
    }

    private void Previous(){
        if(internalAdapter.getItemCount() < 1){
            return;
        }
        boolean isPlaying = mediaPlayer.isPlaying();
        mediaPlayer.stop();

        if(currentTime > 5000){
            if(mediaPlayer.isPlaying()) {
                SetMusic(0);
                SetPlay(true);
            }
            return;
        }

        int filePosition = internalAdapter.getFilePosition(selectedFileName);
        if(filePosition <= 0) {
            selectedFileName = internalAdapter.getFile(internalAdapter.getItemCount()-1).getName();
            SetMusic(0);
        }else{
            SetMusic(-1);
        }

        if(isPlaying) {
            SetPlay(true);
        }
    }

    private void Next(){
        if(internalAdapter.getItemCount() < 1){
            return;
        }
        boolean isPlaying = mediaPlayer.isPlaying();
        mediaPlayer.stop();

        if(internalAdapter.getFilePosition(selectedFileName) >= internalAdapter.getItemCount()-1){
            selectedFileName = internalAdapter.getFile(0).getName();
            SetMusic(0);
        }else {
            SetMusic(1);
        }
        if(isPlaying) {
            SetPlay(true);
        }
    }

    private void SetPlay(boolean isPlay){
        try {
            if (isPlay) {
                imageViewPlay.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_pause, getTheme()));
                mediaPlayer.start();
            } else {
                imageViewPlay.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_play_arrow, getTheme()));
                mediaPlayer.pause();
            }

            PlayerNotification.createNotification(this);
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.SetPlay: " + e.getMessage(), MainActivity.this, R_ID);
        }
    }

    private void SetMusic(int plus){
        try {
            if (internalAdapter.getItemCount() < 1) {
                Handler.ShowSnack("Você ainda não baixou MP3", null, MainActivity.this, R_ID);
            }

            int newPosition = internalAdapter.getFilePosition(selectedFileName) + plus;

            if (newPosition > internalAdapter.getItemCount() - 1 || newPosition < 0) {
                return;
            }

            selectedFileName = internalAdapter.getFile(newPosition).getName();

            if (internalAdapter.getFile(newPosition).exists()) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer = MediaPlayer.create(this, Uri.parse(internalAdapter.getFile(newPosition).getAbsolutePath()));

                finalTime = mediaPlayer.getDuration();
                String Minutes = String.valueOf(TimeUnit.MILLISECONDS.toMinutes((long) finalTime));
                String Seconds = String.valueOf(TimeUnit.MILLISECONDS.toSeconds((long) finalTime)
                        - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) finalTime)));
                if(Seconds.length() < 2){
                    Seconds = "0"+Seconds;
                }
                textViewTotalTime.setText(String.format("%s:%s", Minutes, Seconds));

                GetMusicInfo(internalAdapter.getFile(newPosition).getAbsolutePath());

                myHandler.postDelayed(UpdateSongTime,100);
            }

            SavePreferences();
            getFavorite();
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.SetMusic: " + e.getMessage(), MainActivity.this, R_ID);
        }
    }

    private void SetRecyclerView(boolean isExternal){
        try {
            recyclerViewMusicList.addOnItemTouchListener(new RecyclerItemClickListener(
                    this, recyclerViewMusicList, new RecyclerItemClickListener.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {

                    try {
                        if (!isExternal) {
                            mediaPlayer.stop();
                            selectedFileName = internalAdapter.getFile(position).getName();
                            internalAdapter.notifyDataSetChanged();
                            SetMusic(0);
                            SetPlay(true);
                        } else {
                            if (externalAdapter.getFileName(position).equals(selectedFileName))
                                return;

                            File file = new File(
                                    Objects.requireNonNull(getExternalFilesDir(Environment.DIRECTORY_MUSIC)).getAbsolutePath(),
                                    externalAdapter.getDataInfo(position).getAsJsonObject().get("filename").getAsString()
                            );

                            if (file.exists()) {
                                mediaPlayer.stop();
                                selectedFileName = file.getName();
                                SetMusic(0);
                                SetPlay(true);
                                externalAdapter.notifyDataSetChanged();
                                return;
                            }

                            imageViewFavorite.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.ic_favorite_border,getTheme()));
                            if (externalAdapter == null) return;
                            GetExternalMusicInfo(position);
                            StreamPlay(externalAdapter.getFileName(position));
                            SetPlay(true);
                            selectedFileName = externalAdapter.getFileName(position);
                            externalAdapter.notifyDataSetChanged();
                        }
                    }catch (Exception e){
                        Handler.ShowSnack("Houve um erro","MainActivity.SetRecyclerView.onItemClick: " + e.getMessage(), MainActivity.this, R_ID);
                    }
                }

                @Override
                public boolean onLongItemClick(View view, int position) {
                    return false;
                }
            }));
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.SetRecyclerView: " + e.getMessage(), MainActivity.this, R_ID);
        }
    }

    private void SetInternalSearchView(){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        //int width = displayMetrics.widthPixels;
        //searchView.setMaxWidth(width-150);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (!(internalAdapter == null)) {
                    internalAdapter.getFilter().filter(s);
                }
                return false;
            }
        });
    }

    private void SetExternalSearchView(){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        searchView.setMaxWidth(width-150);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (!(externalAdapter == null)) {
                    externalAdapter.getFilter().filter(s);
                }
                return false;
            }
        });
    }

    private void Shuffle(){
        if(imageViewShuffleCheck.getVisibility() == View.INVISIBLE){
            imageViewShuffleCheck.setVisibility(View.VISIBLE);
            internalAdapter.setShuffle(true);
        }else{
            imageViewShuffleCheck.setVisibility(View.INVISIBLE);
            internalAdapter.setShuffle(false);
        }
    }

    private void GetInternalMusicList(){
        try {
            File dir = new File(path);
            File[] fileList = dir.listFiles();

            List<File> files = new ArrayList<>();

            assert fileList != null;
            Collections.addAll(files, fileList);

            internalAdapter = new InternalMusicListAdapter(files, this, R_ID);

            //assert fileList != null;
            //Arrays.sort(files);
            Collections.sort(files);
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.GetInternalMusicList: " + e.getMessage(), MainActivity.this, R_ID);
        }
    }

    private void GetExternalMusicList(){
        try {
            imageViewExternalMusicList.setEnabled(false);
            imageViewExternalMusicList.setVisibility(View.INVISIBLE);
            progress.setVisibility(View.VISIBLE);

            Call<JsonObject> call = musicListInterface.GetMusicList();
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    try {
                        if (!Handler.isRequestError(response, MainActivity.this, R_ID)){
                            JsonObject jsonObject = response.body();
                            assert jsonObject != null;
                            JsonArray data = jsonObject.get("data").getAsJsonArray();

                            DialogExternalMusicList(data);
                        }
                    }catch (Exception e){
                        Handler.ShowSnack("Houve um erro","MainActivity.GetExternalMusicList.onResponse: " + e.getMessage(), MainActivity.this, R_ID);
                    }

                    imageViewExternalMusicList.setEnabled(true);
                    imageViewExternalMusicList.setVisibility(View.VISIBLE);
                    progress.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                    Handler.ShowSnack("Houve um erro","MainActivity.GetExternalMusicList.onFailure: " + t.toString(), MainActivity.this, R_ID);

                    imageViewExternalMusicList.setEnabled(true);
                    imageViewExternalMusicList.setVisibility(View.VISIBLE);
                    progress.setVisibility(View.INVISIBLE);
                }
            });

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.GetExternalMusicList: " + e.getMessage(), MainActivity.this, R_ID);

            imageViewExternalMusicList.setEnabled(true);
            imageViewExternalMusicList.setVisibility(View.VISIBLE);
            progress.setVisibility(View.INVISIBLE);
        }
    }

    private void GetMusicInfo(String source){
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(source);

        try {
            byte[] art = mediaMetadataRetriever.getEmbeddedPicture();
            assert art != null;
            Bitmap artImage = BitmapFactory.decodeByteArray(art, 0, art.length);
            imageViewArt.setImageBitmap(artImage);
            textViewArtistName.setText(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
            textViewMusicName.setText(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));

            musicInfo = new JsonObject();
            musicInfo.addProperty("artist", mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
            musicInfo.addProperty("title", mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
            String artString = Handler.ImageEncode(artImage);
            musicInfo.addProperty("art", artString);
        } catch (Exception e) {
            imageViewArt.setImageBitmap(null);
            String unknown = "Arquivo sem informações";
            String filename = source.replace(path,"").replace("/","").replace(".mp3","").replace(" - ","\n");
            textViewArtistName.setText(filename);
            textViewMusicName.setText(unknown);

            musicInfo = new JsonObject();
            musicInfo.addProperty("artist", filename);
            musicInfo.addProperty("title", unknown);
            //jsonObject.addProperty("art", Arrays.toString(art));
        }
    }

    private void GetExternalMusicInfo(int position){
        try {
            JsonObject jsonObject = externalAdapter.getDataInfo(position);
            textViewArtistName.setText(jsonObject.get("artist").getAsString());
            textViewMusicName.setText(jsonObject.get("title").getAsString());

            imageViewArt.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.download, getTheme()));

            musicInfo = new JsonObject();
            musicInfo.addProperty("artist", jsonObject.get("artist").getAsString());
            musicInfo.addProperty("title", jsonObject.get("title").getAsString());

            /*if (jsonObject.has("art")) {
                imageViewArt.setImageBitmap(Handler.ImageDecode(jsonObject.get("art").getAsString()));
                musicInfo.addProperty("art", jsonObject.get("art").getAsString());
            } else {
                imageViewArt.setImageBitmap(null);
            }*/
            GetExternalMusicArt(jsonObject.get("filename").getAsString());
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.GetExternalMusicInfo: " + e.getMessage(), MainActivity.this, R_ID);
        }
    }

    private void DialogInternalMusicList(){
        try {
            if(internalAdapter.getItemCount() < 1){
                Handler.ShowSnack("Lista vazia",null, this, R_ID);
                return;
            }

            Dialog dialogMusicList = new Dialog(this, R.style.Theme_AppCompat_Dialog_MinWidth);
            dialogMusicList.setContentView(R.layout.dialog_internal_music_list);

            recyclerViewMusicList = dialogMusicList.findViewById(R.id.dialogInternalMusic_RecyclerView);
            swipeRefresh = dialogMusicList.findViewById(R.id.dialogInternalMusic_SwipeRefresh);
            searchView = dialogMusicList.findViewById(R.id.dialogInternalMusic_SearchView);
            ImageView imageViewList = dialogMusicList.findViewById(R.id.dialogInternalMusic_ImageViewList);
            ImageView imageViewFavorite = dialogMusicList.findViewById(R.id.dialogInternalMusic_ImageViewFavorite);

            RecyclerView.LayoutManager layoutManager;
            recyclerViewMusicList.setHasFixedSize(true);
            layoutManager = new LinearLayoutManager(this);
            recyclerViewMusicList.setLayoutManager(layoutManager);

            swipeRefresh.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.colorAccent));
            swipeRefresh.setColorSchemeColors(getResources().getColor(R.color.colorBlack));
            swipeRefresh.setOnRefreshListener(this::SwipeRefreshAction);

            recyclerViewMusicList.setAdapter(internalAdapter);

            if(favoriteList.size()<1){
                imageViewFavorite.setAlpha(0.6f);
            }

            imageViewFavorite.setOnClickListener(v->{
                if(favoriteList.size()<1)return;
                internalAdapter = new InternalMusicListAdapter(favoriteList,this, R_ID);
                recyclerViewMusicList.setAdapter(internalAdapter);
            });

            imageViewList.setOnClickListener(v->{
                GetInternalMusicList();
                recyclerViewMusicList.setAdapter(internalAdapter);
            });

            dialogMusicList.create();
            dialogMusicList.show();

            SetRecyclerView(false);
            SetInternalSearchView();

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.DialogMusicList: " + e.getMessage(), this, R_ID);
        }
    }

    private void DialogExternalMusicList(JsonArray data){
        try {
            Dialog dialogMusicList = new Dialog(this, R.style.Theme_AppCompat_Dialog_MinWidth);
            dialogMusicList.setContentView(R.layout.dialog_external_music_list);

            recyclerViewMusicList = dialogMusicList.findViewById(R.id.dialogExternalMusic_RecyclerView);
            searchView = dialogMusicList.findViewById(R.id.dialogExternalMusic_SearchView);

            RecyclerView.LayoutManager layoutManager;
            recyclerViewMusicList.setHasFixedSize(true);
            layoutManager = new LinearLayoutManager(this);
            recyclerViewMusicList.setLayoutManager(layoutManager);

            externalAdapter = new ExternalMusicListAdapter(internalAdapter.getFiles(), data, this, R.id.activityMain_ImageView_Play);
            //externalAdapter.setSelectedFileName(selectedFileName);

            recyclerViewMusicList.setAdapter(externalAdapter);

            dialogMusicList.create();
            dialogMusicList.show();

            SetRecyclerView(true);
            SetExternalSearchView();

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.DialogExternalMusicList: " + e.getMessage(), MainActivity.this, R_ID);
        }
    }

    private void SwipeRefreshAction(){
        GetInternalMusicList();
        Shuffle();
        recyclerViewMusicList.setAdapter(internalAdapter);
        swipeRefresh.setRefreshing(false);
    }

    private void StreamPlay(String song){
        try {
            String url = ApiClient.BASE_URL+song;

            mediaPlayer.stop();
            mediaPlayer = new MediaPlayer();

            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepare();
            finalTime = mediaPlayer.getDuration();

            finalTime = mediaPlayer.getDuration();
            String Minutes = String.valueOf(TimeUnit.MILLISECONDS.toMinutes((long) finalTime));
            String Seconds = String.valueOf(TimeUnit.MILLISECONDS.toSeconds((long) finalTime)
                    - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) finalTime)));
            if(Seconds.length() < 2){
                Seconds = "0"+Seconds;
            }
            textViewTotalTime.setText(String.format("%s:%s", Minutes, Seconds));

            myHandler.postDelayed(UpdateSongTime,100);
            //mediaPlayer.start();
        } catch (Exception e) {
            Handler.ShowSnack("Houve um erro","MainActivity.StreamPlay: " + e.getMessage(), MainActivity.this, R_ID);
        }
    }

    private void SavePreferences(){
        SharedPreferences settings = getSharedPreferences(PREFERENCES, 0);
        SharedPreferences.Editor editor = settings.edit();
        boolean shuffle = false;
        if(imageViewShuffleCheck.getVisibility()==View.VISIBLE){
            shuffle = true;
        }
        editor.putBoolean("shuffle", shuffle);
        editor.putString("repeat", textViewOneOrALL.getText().toString());
        editor.putString("selectedFileName", selectedFileName);
        editor.apply();
    }

    private void LoadPreferences(){
        SharedPreferences settings = getSharedPreferences(PREFERENCES, 0);
        boolean shuffle = settings.getBoolean("shuffle",false);
        String repeat = settings.getString("repeat","N");

        assert repeat != null;
        if(!repeat.equalsIgnoreCase("N")){
            imageViewRepeatCheck.setVisibility(View.VISIBLE);
            textViewOneOrALL.setVisibility(View.VISIBLE);
            textViewOneOrALL.setText(repeat);
        }

        if(!shuffle){
            imageViewShuffleCheck.setVisibility(View.VISIBLE);
        }

        selectedFileName = settings.getString("selectedFileName","");

        JsonParser parser = new JsonParser();
        JsonArray jsonArray;
        String favorite = settings.getString("favorite","");
        if(favorite.equals("")) return;
        jsonArray = (JsonArray) parser.parse(settings.getString("favorite",""));
        String path = Objects.requireNonNull(getExternalFilesDir(Environment.DIRECTORY_MUSIC)).getPath();
        for (JsonElement jsonElement: jsonArray){
            File file = new File(path,jsonElement.getAsJsonObject().get("file").getAsString());
            favoriteList.add(file);
        }

    }

    private void getFavorite(){
        try {
            boolean isFavorite = false;
            for (int i = 0; i < favoriteList.size(); i++) {
                if (favoriteList.get(i).getName().equals(selectedFileName)) isFavorite = true;
            }

            if (!isFavorite) {
                imageViewFavorite.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.ic_favorite_border,getTheme()));
                //imageViewFavorite.setImageDrawable(getResources().getDrawable(R.drawable.ic_favorite_border));
            } else {
                imageViewFavorite.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.ic_favorite,getTheme()));
                //imageViewFavorite.setImageDrawable(getResources().getDrawable(R.drawable.ic_favorite));
            }
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.getFavorite: " + e.getMessage(), MainActivity.this, R_ID);
        }
    }

    private void setFavorite(){
        try {
            boolean isFavorite = false;
            for (int i = 0; i < favoriteList.size(); i++) {
                if (favoriteList.get(i).getName().equals(selectedFileName)) isFavorite = true;
            }

            int position = internalAdapter.getFilePosition(selectedFileName);
            File file = internalAdapter.getFile(position);

            if (isFavorite) {
                favoriteList.remove(file);
                imageViewFavorite.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.ic_favorite_border,getTheme()));
                //imageViewFavorite.setImageDrawable(getResources().getDrawable(R.drawable.ic_favorite_border));
            } else {
                favoriteList.add(file);
                imageViewFavorite.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.ic_favorite,getTheme()));
                //imageViewFavorite.setImageDrawable(getResources().getDrawable(R.drawable.ic_favorite));
            }

            SharedPreferences settings = getSharedPreferences(PREFERENCES, 0);
            SharedPreferences.Editor editor = settings.edit();
            JsonArray jsonArray = new JsonArray();

            for (File infile : favoriteList) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("file", infile.getName());
                jsonArray.add(jsonObject);
            }
            editor.putString("favorite", jsonArray.toString());
            editor.apply();
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.setFavorite: " + e.getMessage(), MainActivity.this, R_ID);
        }
    }

    public static boolean isPlaying(){
        return mediaPlayer.isPlaying();
    }

    private void GetExternalMusicArt(String filename){
        try {
            Call<JsonObject> call = musicListInterface.GetFullMusicArt(filename,true);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    try {
                        if (!Handler.isRequestError(response, MainActivity.this, R_ID)){
                            JsonObject jsonObject = response.body();
                            assert jsonObject != null;
                            JsonArray jsonArray = jsonObject.get("data").getAsJsonArray();

                            imageViewArt.setImageBitmap(Handler.ImageDecode(jsonArray.get(0).getAsJsonObject().get("art").getAsString()));
                            musicInfo.addProperty("art", jsonArray.get(0).getAsJsonObject().get("art").getAsString());
                            PlayerNotification.createNotification(MainActivity.this);
                        }else{
                            imageViewArt.setImageBitmap(null);
                        }
                    }catch (Exception e){
                        Handler.ShowSnack("Houve um erro","MainActivity.GetExternalMusicArt.onResponse: " + e.getMessage(), MainActivity.this, R_ID);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                    Handler.ShowSnack("Houve um erro","MainActivity.GetExternalMusicArt.onFailure: " + t.toString(), MainActivity.this, R_ID);
                }
            });

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.GetExternalMusicArt: " + e.getMessage(), MainActivity.this, R_ID);
        }
    }

    private final Runnable UpdateSongTime = new Runnable() {
        @SuppressLint("DefaultLocale")
        public void run() {
            currentTime = mediaPlayer.getCurrentPosition();

            if(textViewOneOrALL.getText().toString().equalsIgnoreCase("1")){
                int current = mediaPlayer.getCurrentPosition();
                int duration = mediaPlayer.getDuration();
                if(current >= duration - 1000){
                    mediaPlayer.seekTo(0);
                }
            }

            if(textViewOneOrALL.getText().toString().equalsIgnoreCase("A")){
                int current = mediaPlayer.getCurrentPosition();
                int duration = mediaPlayer.getDuration();
                if(current >= duration - 1000){
                    Next();
                }
            }

            if(textViewOneOrALL.getText().toString().equalsIgnoreCase("N")){
                int current = mediaPlayer.getCurrentPosition();
                int duration = mediaPlayer.getDuration();
                if(current >= duration - 1000){
                    SetPlay(false);
                    currentTime=0;
                    mediaPlayer.seekTo(0);
                }
            }

            String Minutes = String.valueOf(TimeUnit.MILLISECONDS.toMinutes((long) currentTime));
            String Seconds = String.valueOf(TimeUnit.MILLISECONDS.toSeconds((long) currentTime)
                    - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) currentTime)));
            if(Seconds.length() < 2){
                Seconds = "0"+Seconds;
            }
            textViewCurrentTime.setText(String.format("%s:%s", Minutes, Seconds));

            seekbar.setMax((int) finalTime);
            seekbar.setProgress((int)currentTime);

            if(!mediaPlayer.isPlaying()){
                imageViewPlay.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.ic_play_arrow,getTheme()));
                //imageViewPlay.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow));
            }

            myHandler.postDelayed(this, 100);
        }
    };

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getExtras().getString("actionName");

            switch (action){
                case PlayerNotification.ACTION_PREVIOUS:
                    Previous();
                    PlayerNotification.createNotification(context);
                    break;
                case PlayerNotification.ACTION_PLAY:
                    Play();
                    PlayerNotification.createNotification(context);
                    break;
                case PlayerNotification.ACTION_NEXT:
                    Next();
                    PlayerNotification.createNotification(context);
                    break;
            }
        }

    };
}