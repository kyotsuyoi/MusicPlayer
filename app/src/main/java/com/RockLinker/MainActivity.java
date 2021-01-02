package com.RockLinker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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

import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.RockLinker.CommonClasses.ApiClient;
import com.RockLinker.Services.HeadphoneButtonService;
import com.RockLinker.Services.HeadphonePlugService;
import com.RockLinker.Services.OnClearFormRecentService;
import com.RockLinker.CommonClasses.PlayerNotification;
import com.RockLinker.CommonClasses.RecyclerItemClickListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import at.markushi.ui.CircleButton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String PREFERENCES = "ROCKLINKER_PREFERENCES";

    private CircleButton buttonNext, buttonPlay, buttonPrevious;
    private CircleButton buttonFavorite, buttonExternalMusicList, buttonInternalMusicList;
    private CircleButton buttonShuffle, buttonRepeat;

    private ImageView imageViewArt;
    private static MediaPlayer mediaPlayer;

    private boolean isShuffle = false;
    private String repeat = "N";

    private ProgressBar progress;

    private double currentTime = 0;
    private double duration = 0;

    private final Handler myHandler = new Handler();
    private SeekBar seekbar;
    private TextView textViewCurrentTime, textViewTotalTime, textViewMusicName, textViewArtistName;

    private SearchView searchView;

    private String path;

    private RecyclerView recyclerViewMusicList;
    private SwipeRefreshLayout swipeRefreshInternal, swipeRefreshExternal;
    private InternalMusicListAdapter internalMusicListAdapter;
    private ExternalMusicListAdapter externalMusicListAdapter;
    private InternalArtistListAdapter internalArtistListAdapter;

    private final com.RockLinker.CommonClasses.Handler Handler = new com.RockLinker.CommonClasses.Handler();
    private final int R_ID = R.id.activityMain_Button_Play;
    private final MusicListInterface musicListInterface = ApiClient.getApiClient().create(MusicListInterface.class);

    public static String selectedFileName;
    private List<File> favoriteList;

    private NotificationManager notificationManager;

    public static JsonObject musicInfo;

    private long downloadID;

    private final HeadphonePlugService servicePlug = new HeadphonePlugService();
    //private final HeadphoneButtonService serviceButton = new HeadphoneButtonService();

    private AudioManager audioManager;
    private ComponentName receiverComponent;

    private int listType = 0;
    //Type 1 to internal music list
    //Type 2 to internal music artist
    //Type 3 to external music list

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_main);

            mediaPlayer = new MediaPlayer();

            buttonNext = findViewById(R.id.activityMain_Button_Next);
            buttonPlay = findViewById(R.id.activityMain_Button_Play);
            buttonPrevious = findViewById(R.id.activityMain_Button_Previous);
            imageViewArt = findViewById(R.id.activityMain_ImageView_Art);

            textViewCurrentTime = findViewById(R.id.activityMain_TextView_CurrentTime);
            textViewTotalTime = findViewById(R.id.activityMain_TextView_TotalTime);
            textViewMusicName = findViewById(R.id.activityMain_TextView_MusicName);
            textViewArtistName = findViewById(R.id.activityMain_TextView_ArtistName);

            buttonShuffle = findViewById(R.id.activityMain_Button_Shuffle);
            buttonRepeat = findViewById(R.id.activityMain_Button_Repeat);

            buttonInternalMusicList = findViewById(R.id.activityMain_Button_InternalMusicList);
            buttonExternalMusicList = findViewById(R.id.activityMain_Button_ExternalMusicList);

            buttonFavorite = findViewById(R.id.activityMain_Button_Favorite);

            progress = findViewById(R.id.activityMain_ImageView_ProgressBar);
            progress.setVisibility(View.INVISIBLE);

            seekbar = findViewById(R.id.activityMain_SeekBar);
            seekbar.setClickable(false);

            buttonPlay.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_play_arrow_purple, getTheme()));

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

            myHandler.postDelayed(UpdateSongTime, 100);

            registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            registerReceiver(servicePlug, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
            //registerReceiver(serviceButton, new IntentFilter(Intent.ACTION_MEDIA_BUTTON));

            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            receiverComponent = new ComponentName(this, HeadphoneButtonService.class);
            audioManager.registerMediaButtonEventReceiver(receiverComponent);

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.onCreate: " + e.getMessage(), this, R_ID);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        notificationManager.cancelAll();

        unregisterReceiver(onDownloadComplete);
        unregisterReceiver(broadcastReceiver);
        unregisterReceiver(servicePlug);
        //unregisterReceiver(serviceButton);
        audioManager.unregisterMediaButtonEventReceiver(receiverComponent);

        SharedPreferences settings = getSharedPreferences(PREFERENCES, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("audioID", mediaPlayer.getAudioSessionId());
        editor.apply();
        mediaPlayer.stop();

        super.onDestroy();
    }

    /*@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_HEADSETHOOK){
            Play();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }*/

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

            registerReceiver(broadcastReceiver, new IntentFilter("broadcastAction"));
            startService(new Intent(getBaseContext(), OnClearFormRecentService.class));
        }
    }

    private void SetButtons(){

        buttonPlay.setOnClickListener(v -> Play());

        buttonNext.setOnClickListener(v -> Next());

        buttonPrevious.setOnClickListener(v -> Previous());

        buttonRepeat.setOnClickListener(v -> {

            switch (repeat){
                case "1":
                    repeat="A";
                    buttonRepeat.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_repeat_purple, getTheme()));
                    break;
                case "A":
                    repeat="N";
                    buttonRepeat.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_no_repeat, getTheme()));
                    break;
                case "N":
                    repeat="1";
                    buttonRepeat.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_repeat_one_purple, getTheme()));
                    break;
            }

            SavePreferences();
        });

        buttonShuffle.setOnClickListener(v -> {
            isShuffle = !isShuffle;
            Shuffle();
            SavePreferences();
        });

        buttonInternalMusicList.setOnClickListener(v -> DialogInternalMusicList());

        buttonExternalMusicList.setOnClickListener(v -> {
            GetInternalMusicList();
            GetExternalMusicList();
        });

        buttonFavorite.setOnClickListener(v -> setFavorite());

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

    public void Play(){
        try {
            if(selectedFileName.equals("")) return;
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
        if(internalMusicListAdapter.getItemCount() < 1){
            return;
        }

        if(currentTime > 5000){
            mediaPlayer.seekTo(0);
            return;
        }

        boolean isPlaying = mediaPlayer.isPlaying();
        mediaPlayer.stop();

        int filePosition = internalMusicListAdapter.getFilePosition(selectedFileName);
        if(filePosition <= 0) {
            selectedFileName = internalMusicListAdapter.getFile(internalMusicListAdapter.getItemCount()-1).getName();
            SetMusic(0);
        }else{
            SetMusic(-1);
        }

        if(isPlaying) {
            SetPlay(true);
        }else{
            PlayerNotification.createNotification(this);
        }
    }

    private void Next(){
        if(internalMusicListAdapter.getItemCount() < 1){
            return;
        }

        boolean isPlaying = mediaPlayer.isPlaying();
        mediaPlayer.stop();

        int filePosition =  internalMusicListAdapter.getFilePosition(selectedFileName);
        if(filePosition >= internalMusicListAdapter.getItemCount()-1){
            selectedFileName = internalMusicListAdapter.getFile(0).getName();
            SetMusic(0);
        }else {
            SetMusic(1);
        }
        
        if(isPlaying) {
            SetPlay(true);
        }else{
            PlayerNotification.createNotification(this);
        }
    }

    private void SetPlay(boolean isPlay){
        try {
            if (isPlay) {
                buttonPlay.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_pause_purple, getTheme()));
                mediaPlayer.start();
            } else {
                buttonPlay.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_play_arrow_purple, getTheme()));
                mediaPlayer.pause();
            }

            if(internalMusicListAdapter != null){
                internalMusicListAdapter.notifyDataSetChanged();
            }
            if(externalMusicListAdapter != null){
                externalMusicListAdapter.notifyDataSetChanged();
            }
            PlayerNotification.createNotification(this);
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.SetPlay: " + e.getMessage(), MainActivity.this, R_ID);
        }
    }

    private void SetMusic(int plus){
        try {
            if (internalMusicListAdapter.getItemCount() < 1) {
                Handler.ShowSnack("Você ainda não baixou MP3", null, MainActivity.this, R_ID);
            }

            int newPosition = internalMusicListAdapter.getFilePosition(selectedFileName) + plus;

            if (newPosition > internalMusicListAdapter.getItemCount() - 1 || newPosition < 0) {
                return;
            }

            selectedFileName = internalMusicListAdapter.getFile(newPosition).getName();

            if (internalMusicListAdapter.getFile(newPosition).exists()) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer = MediaPlayer.create(this, Uri.parse(internalMusicListAdapter.getFile(newPosition).getAbsolutePath()));

                duration = mediaPlayer.getDuration();
                String Minutes = String.valueOf(TimeUnit.MILLISECONDS.toMinutes((long) duration));
                String Seconds = String.valueOf(TimeUnit.MILLISECONDS.toSeconds((long) duration)
                        - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) duration)));
                if(Seconds.length() < 2){
                    Seconds = "0"+Seconds;
                }
                textViewTotalTime.setText(String.format("%s:%s", Minutes, Seconds));
                String cur = "0:00";
                textViewCurrentTime.setText(cur);

                musicInfo = getMusicInfo(internalMusicListAdapter.getFile(newPosition).getAbsolutePath());
            }

            SavePreferences();
            getFavorite();
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.SetMusic: " + e.getMessage(), MainActivity.this, R_ID);
        }
    }

    private void setRecyclerView(){
        try {
            recyclerViewMusicList.addOnItemTouchListener(new RecyclerItemClickListener(
                    this, recyclerViewMusicList, new RecyclerItemClickListener.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    try {
                        switch (listType){
                            case 1:
                                mediaPlayer.stop();
                                selectedFileName = internalMusicListAdapter.getFile(position).getName();
                                internalMusicListAdapter.notifyDataSetChanged();
                                SetMusic(0);
                                SetPlay(true);
                                break;
                            case 2:
                                internalMusicListAdapter.getFilter().filter(internalArtistListAdapter.getArtistName(position));
                                listType =1;
                                recyclerViewMusicList.setAdapter(internalMusicListAdapter);
                                break;
                            case 3:
                                if (externalMusicListAdapter.getFileName(position).equals(selectedFileName)) return;

                                File file = new File(
                                        Objects.requireNonNull(getExternalFilesDir(Environment.DIRECTORY_MUSIC)).getAbsolutePath(),
                                        externalMusicListAdapter.getDataInfo(position).getAsJsonObject().get("filename").getAsString()
                                );

                                if (file.exists()) {
                                    mediaPlayer.stop();
                                    selectedFileName = file.getName();
                                    SetMusic(0);
                                    SetPlay(true);
                                    externalMusicListAdapter.notifyDataSetChanged();
                                    return;
                                }

                                buttonFavorite.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.ic_favorite_border_purple,getTheme()));
                                if (externalMusicListAdapter == null) return;
                                GetExternalMusicInfo(position);
                                StreamPlay(externalMusicListAdapter.getFileName(position));
                                SetPlay(true);
                                selectedFileName = externalMusicListAdapter.getFileName(position);
                                externalMusicListAdapter.notifyDataSetChanged();
                                break;
                        }
                    }catch (Exception e){
                        Handler.ShowSnack("Houve um erro","MainActivity.SetRecyclerView.onItemClick: " + e.getMessage(), MainActivity.this, R_ID);
                    }
                }

                @Override
                public boolean onLongItemClick(View view, int position) {
                    DialogMusicMenu(position);
                    return true;
                }
            }));
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.SetRecyclerView: " + e.getMessage(), MainActivity.this, R_ID);
        }
    }

    private void SetInternalSearchView(){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        searchView.setMaxWidth(width);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (!(internalMusicListAdapter == null)) {
                    internalMusicListAdapter.getFilter().filter(s);
                }
                return false;
            }
        });
    }

    private void SetExternalSearchView(){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        searchView.setMaxWidth(width);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                if (!(externalMusicListAdapter == null)) {
                    externalMusicListAdapter.getFilter().filter(s);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if(externalMusicListAdapter != null){
                    if(s.equals("")){
                        externalMusicListAdapter.getFilter().filter("");
                    }
                }
                return false;
            }
        });
    }

    private void Shuffle(){
        if(isShuffle){
            buttonShuffle.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_transform_purple, getTheme()));
            internalMusicListAdapter.setShuffle(true);
        }else{
            buttonShuffle.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_no_transform, getTheme()));
            internalMusicListAdapter.setShuffle(false);
        }
    }

    private void GetInternalMusicList(){
        try {
            File dir = new File(path);
            File[] fileList = dir.listFiles();

            List<File> files = new ArrayList<>();

            assert fileList != null;
            Collections.addAll(files, fileList);

            internalMusicListAdapter = new InternalMusicListAdapter(files, this, R_ID);

            //assert fileList != null;
            //Arrays.sort(files);
            Collections.sort(files);
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.GetInternalMusicList: " + e.getMessage(), MainActivity.this, R_ID);
        }
    }

    private JsonArray getInternalArtistList(){
        JsonArray jsonArray = new JsonArray();
        try {
            File dir = new File(path);
            File[] fileList = dir.listFiles();

            List<File> files = new ArrayList<>();

            assert fileList != null;
            Collections.addAll(files, fileList);

            for (File file : files) {
                JsonObject jsonObject = getSingleMusicInfo(file.getAbsolutePath());
                boolean isFind = false;
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject innerObject = jsonArray.get(i).getAsJsonObject();
                    if (innerObject.get("artist").getAsString().equals(jsonObject.get("artist").getAsString())) {
                        int quantity = jsonArray.get(i).getAsJsonObject().get("quantity").getAsInt();
                        quantity++;
                        jsonArray.get(i).getAsJsonObject().addProperty("quantity", quantity);
                        isFind = true;
                        files.remove(file);
                        i = jsonArray.size();
                    }
                }

                if (!isFind) {
                    JsonObject newJson = new JsonObject();
                    newJson.addProperty("artist", jsonObject.get("artist").getAsString());
                    newJson.addProperty("quantity", 1);
                    newJson.addProperty("art",  jsonObject.get("art").getAsString());
                    jsonArray.add(newJson);
                }

            }
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.getInternalArtistList: " + e.getMessage(), this, R_ID);
        }

        return jsonArray;
    }

    private void GetExternalMusicList(){
        try {
            buttonExternalMusicList.setEnabled(false);
            buttonExternalMusicList.setVisibility(View.INVISIBLE);
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

                    progress.setVisibility(View.INVISIBLE);
                    buttonExternalMusicList.setVisibility(View.VISIBLE);
                    buttonExternalMusicList.setAlpha(0.3f);
                    Thread t = new Thread(() -> {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        runOnUiThread(() -> {
                            buttonExternalMusicList.setAlpha(1f);
                            buttonExternalMusicList.setEnabled(true);
                        });
                    });
                    t.start();
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                    Handler.ShowSnack("Houve um erro","MainActivity.GetExternalMusicList.onFailure: " + t.toString(), MainActivity.this, R_ID);

                    buttonExternalMusicList.setEnabled(true);
                    buttonExternalMusicList.setVisibility(View.VISIBLE);
                    progress.setVisibility(View.INVISIBLE);
                }
            });

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.GetExternalMusicList: " + e.getMessage(), MainActivity.this, R_ID);

            buttonExternalMusicList.setEnabled(true);
            buttonExternalMusicList.setVisibility(View.VISIBLE);
            progress.setVisibility(View.INVISIBLE);
        }
    }

    private JsonObject getSingleMusicInfo(String source){
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(source);

        JsonObject jsonObject = new JsonObject();
        try {
            jsonObject.addProperty("artist", mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));

            byte[] art = mediaMetadataRetriever.getEmbeddedPicture();
            assert art != null;
            Bitmap artImage = BitmapFactory.decodeByteArray(art, 0, art.length);
            String artString = Handler.ImageEncode(artImage);
            jsonObject.addProperty("art", artString);
        } catch (Exception e) {
            String unknown = "Arquivo sem informações";

            jsonObject.addProperty("artist", unknown);
        }
        return jsonObject;
    }

    private JsonObject getMusicInfo(String source){
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(source);

        JsonObject jsonObject = new JsonObject();
        try {
            jsonObject.addProperty("artist", mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
            jsonObject.addProperty("title", mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));

            byte[] art = mediaMetadataRetriever.getEmbeddedPicture();
            assert art != null;
            Bitmap artImage = BitmapFactory.decodeByteArray(art, 0, art.length);
            imageViewArt.setImageBitmap(artImage);
            textViewArtistName.setText(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
            textViewMusicName.setText(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
            String artString = Handler.ImageEncode(artImage);
            jsonObject.addProperty("art", artString);

        } catch (Exception e) {
            imageViewArt.setImageBitmap(null);
            String unknown = "Arquivo sem informações";
            String filename = source.replace(path,"").replace("/","").replace(".mp3","").replace(" - ","\n");
            textViewArtistName.setText(filename);
            textViewMusicName.setText(unknown);

            jsonObject.addProperty("artist", filename);
            jsonObject.addProperty("title", unknown);
            //jsonObject.addProperty("art", Arrays.toString(art));
        }
        return jsonObject;
    }

    private void GetExternalMusicInfo(int position){
        try {
            JsonObject jsonObject = externalMusicListAdapter.getDataInfo(position);
            textViewArtistName.setText(jsonObject.get("artist").getAsString());
            textViewMusicName.setText(jsonObject.get("title").getAsString());

            imageViewArt.setImageDrawable(null);

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
            if(internalMusicListAdapter.getItemCount() < 1){
                Handler.ShowSnack("Lista vazia",null, this, R_ID);
                return;
            }

            Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setContentView(R.layout.dialog_internal_music_list);

            recyclerViewMusicList = dialog.findViewById(R.id.dialogInternalMusic_RecyclerView);
            setRecyclerView();
            searchView = dialog.findViewById(R.id.dialogInternalMusic_SearchView);

            CircleButton buttonList = dialog.findViewById(R.id.dialogInternalMusic_Button_List);
            CircleButton buttonFavorite = dialog.findViewById(R.id.dialogInternalMusic_Button_Favorite);
            CircleButton buttonArtist = dialog.findViewById(R.id.dialogInternalMusic_Button_Artist);

            RecyclerView.LayoutManager layoutManager;
            recyclerViewMusicList.setHasFixedSize(true);
            layoutManager = new LinearLayoutManager(this);
            recyclerViewMusicList.setLayoutManager(layoutManager);

            swipeRefreshInternal = dialog.findViewById(R.id.dialogInternalMusic_SwipeRefresh);
            swipeRefreshInternal.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.colorAccent, getTheme()));
            swipeRefreshInternal.setColorSchemeColors(getResources().getColor(R.color.colorBlack, getTheme()));
            swipeRefreshInternal.setOnRefreshListener(this::SwipeRefreshInternalAction);

            JsonArray jsonArray = getInternalArtistList();
            if(listType==0){
                internalArtistListAdapter = new InternalArtistListAdapter(jsonArray,this, R_ID);
                listType=2;
                recyclerViewMusicList.setAdapter(internalArtistListAdapter);
            }else{
                listType=1;
                recyclerViewMusicList.setAdapter(internalMusicListAdapter);
            }

            if(favoriteList.size()<1){
                buttonFavorite.setAlpha(0.6f);
            }

            buttonFavorite.setOnClickListener(v->{
                if(favoriteList.size()<1)return;
                listType=1;
                internalMusicListAdapter = new InternalMusicListAdapter(favoriteList,this, R_ID);
                recyclerViewMusicList.setAdapter(internalMusicListAdapter);
            });

            buttonList.setOnClickListener(v->{
                GetInternalMusicList();
                listType=1;
                recyclerViewMusicList.setAdapter(internalMusicListAdapter);
            });

            buttonArtist.setOnClickListener(v->{
                internalArtistListAdapter = new InternalArtistListAdapter(jsonArray,this, R_ID);
                listType=2;
                recyclerViewMusicList.setAdapter(internalArtistListAdapter);
            });

            dialog.create();
            dialog.show();

            SetInternalSearchView();

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.DialogMusicList: " + e.getMessage(), this, R_ID);
        }
    }

    private void DialogExternalMusicList(JsonArray data){
        try {
            Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setContentView(R.layout.dialog_external_music_list);

            recyclerViewMusicList = dialog.findViewById(R.id.dialogExternalMusic_RecyclerView);
            setRecyclerView();
            searchView = dialog.findViewById(R.id.dialogExternalMusic_SearchView);

            RecyclerView.LayoutManager layoutManager;
            recyclerViewMusicList.setHasFixedSize(true);
            layoutManager = new LinearLayoutManager(this);
            recyclerViewMusicList.setLayoutManager(layoutManager);

            swipeRefreshExternal = dialog.findViewById(R.id.dialogExternalMusic_SwipeRefresh);
            swipeRefreshExternal.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.colorAccent, getTheme()));
            swipeRefreshExternal.setColorSchemeColors(getResources().getColor(R.color.colorBlack, getTheme()));
            swipeRefreshExternal.setOnRefreshListener(this::SwipeRefreshExternalAction);

            externalMusicListAdapter = new ExternalMusicListAdapter(internalMusicListAdapter.getFiles(), data, this, R.id.activityMain_Button_Play);

            recyclerViewMusicList.setAdapter(externalMusicListAdapter);

            dialog.create();
            dialog.show();

            listType=3;
            SetExternalSearchView();

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.DialogExternalMusicList: " + e.getMessage(), MainActivity.this, R_ID);
        }
    }

    private void DialogMusicMenu(int position){
        try {
            Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setContentView(R.layout.dialog_music_menu);

            TextView textViewTitle = dialog.findViewById(R.id.dialogMusicMenu_TextView_Title);
            TextView textViewArtist = dialog.findViewById(R.id.dialogMusicMenu_TextView_Artist);
            TextView textViewYear = dialog.findViewById(R.id.dialogMusicMenu_TextView_Year);
            ImageView imageView = dialog.findViewById(R.id.dialogMusicMenu_ImageView);
            Button button = dialog.findViewById(R.id.dialogMusicMenu_Button);
            CircleButton buttonDownload = dialog.findViewById(R.id.dialogMusicMenu_Button_Download);

            JsonObject jsonObject = externalMusicListAdapter.getDataInfo(position);
            textViewTitle.setText(jsonObject.get("title").getAsString());
            textViewArtist.setText(jsonObject.get("artist").getAsString());

            String year = "Ano desconhecido";
            textViewYear.setText(year);
            if(jsonObject.get("year") != JsonNull.INSTANCE){
                textViewYear.setText(jsonObject.get("year").getAsString());
            }

            imageView.setImageBitmap(Handler.ImageDecode(jsonObject.get("art").getAsString()));

            File file = new File(Objects.requireNonNull(getExternalFilesDir(Environment.DIRECTORY_MUSIC)).getAbsolutePath(), jsonObject.get("filename").getAsString());

            if(file.exists()){
                buttonDownload.setEnabled(false);
                buttonDownload.setVisibility(View.INVISIBLE);
            }else{
                buttonDownload.setOnClickListener(v->{
                    beginDownload(jsonObject.get("filename").getAsString());
                    buttonDownload.setEnabled(false);
                    buttonDownload.setVisibility(View.INVISIBLE);
                });
            }

            button.setOnClickListener(v-> dialog.cancel());

            dialog.create();
            dialog.show();

        }catch (Exception e){
            Toast.makeText(this,"Houve um erro",Toast.LENGTH_LONG).show();
            Handler.ShowSnack("Houve um erro","MainActivity.DialogMusicMenu: " + e.getMessage(), MainActivity.this, R_ID);
        }
    }

    private void SwipeRefreshInternalAction(){
        GetInternalMusicList();
        listType=1;
        recyclerViewMusicList.setAdapter(internalMusicListAdapter);
        swipeRefreshInternal.setRefreshing(false);
    }

    private void SwipeRefreshExternalAction(){
        /*GetInternalMusicList();
        recyclerViewMusicList.setAdapter(internalMusicListAdapter);
        swipeRefresh.setRefreshing(false);*/
    }

    private void StreamPlay(String song){
        try {
            String url = ApiClient.BASE_URL+song;

            mediaPlayer.stop();
            mediaPlayer = new MediaPlayer();

            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepare();
            duration = mediaPlayer.getDuration();

            duration = mediaPlayer.getDuration();
            String Minutes = String.valueOf(TimeUnit.MILLISECONDS.toMinutes((long) duration));
            String Seconds = String.valueOf(TimeUnit.MILLISECONDS.toSeconds((long) duration)
                    - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) duration)));
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

        editor.putBoolean("shuffle", isShuffle);
        editor.putString("repeat", repeat);
        editor.putString("selectedFileName", selectedFileName);
        editor.apply();
    }

    private void LoadPreferences(){
        SharedPreferences settings = getSharedPreferences(PREFERENCES, 0);
        isShuffle = settings.getBoolean("shuffle",false);
        repeat = settings.getString("repeat","N");
        mediaPlayer.setAudioSessionId(settings.getInt("audioID",0));

        switch (Objects.requireNonNull(repeat)){
            case "N":
                buttonRepeat.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_no_repeat, getTheme()));
                break;
            case "1":
                buttonRepeat.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_repeat_one_purple, getTheme()));
                break;
            case "A":
                buttonRepeat.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_repeat_purple, getTheme()));
                break;
        }

        if(isShuffle){
            buttonShuffle.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_transform_purple, getTheme()));
        }else{
            buttonShuffle.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_no_transform, getTheme()));
        }

        selectedFileName = settings.getString("selectedFileName","");

        JsonParser parser = new JsonParser();
        JsonArray jsonArray;
        String favorite = settings.getString("favorite","");
        if(Objects.equals(favorite, "")) return;
        jsonArray = (JsonArray) parser.parse(Objects.requireNonNull(settings.getString("favorite", "")));
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
                buttonFavorite.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.ic_favorite_border_purple,getTheme()));
            } else {
                buttonFavorite.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.ic_favorite_purple,getTheme()));
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

            int position = internalMusicListAdapter.getFilePosition(selectedFileName);
            File file = internalMusicListAdapter.getFile(position);

            if (isFavorite) {
                favoriteList.remove(file);
                buttonFavorite.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.ic_favorite_border_purple,getTheme()));
            } else {
                favoriteList.add(file);
                buttonFavorite.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.ic_favorite_purple,getTheme()));
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
            if(!selectedFileName.equals("") /*&& mediaPlayer.isPlaying()*/) {

                currentTime = mediaPlayer.getCurrentPosition();

                switch (repeat) {
                    case "N":
                        if (currentTime >= duration - 1000) {
                            SetPlay(false);
                            currentTime = 0;
                            mediaPlayer.seekTo(0);
                        }
                        break;
                    case "1":
                        if (currentTime >= duration - 1000) {
                            mediaPlayer.seekTo(0);
                        }
                        break;
                    case "A":
                        if (currentTime >= duration - 1000) {
                            Next();
                        }
                        break;
                }

                String Minutes = String.valueOf(TimeUnit.MILLISECONDS.toMinutes((long) currentTime));
                String Seconds = String.valueOf(TimeUnit.MILLISECONDS.toSeconds((long) currentTime)
                        - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) currentTime)));
                if (Seconds.length() < 2) {
                    Seconds = "0" + Seconds;
                }
                textViewCurrentTime.setText(String.format("%s:%s", Minutes, Seconds));

                seekbar.setMax((int) duration);
                seekbar.setProgress((int) currentTime);

                if (!mediaPlayer.isPlaying()) {
                    buttonPlay.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_play_arrow_purple, getTheme()));
                }
            }

            myHandler.postDelayed(this, 100);
        }
    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = Objects.requireNonNull(intent.getExtras()).getString("actionName");
            switch (Objects.requireNonNull(action)){
                case PlayerNotification.ACTION_PREVIOUS:
                    Previous();
                    break;
                case PlayerNotification.ACTION_PLAY:
                case "play_pause":
                    Play();
                    break;
                case PlayerNotification.ACTION_NEXT:
                    Next();
                    break;
                case "pause":
                    SetPlay(false);
                    PlayerNotification.createNotification(context);
                    break;
            }
        }
    };

    private void beginDownload(String filename){
        File file = new File(Objects.requireNonNull(getExternalFilesDir(Environment.DIRECTORY_MUSIC)).getAbsolutePath(),filename);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(ApiClient.BASE_URL+filename))
                .setTitle(filename)
                .setDescription("Baixando")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(file))
                .setRequiresCharging(false)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true);

        DownloadManager downloadManager= (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        downloadID = downloadManager.enqueue(request);
    }

    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadID == id) {
                externalMusicListAdapter.GetInternalMusicList();
                externalMusicListAdapter.notifyDataSetChanged();
            }
        }
    };
}
