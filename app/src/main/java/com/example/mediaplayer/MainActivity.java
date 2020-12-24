package com.example.mediaplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;

import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mediaplayer.CommonClasses.ApiClient;
import com.example.mediaplayer.CommonClasses.RecyclerItemClickListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
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
    private MediaPlayer mediaPlayer;

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

    //public static int oneTimeOnly = 0;

    //private boolean isPaused = true;

    private String path;
    //private List<File> files;
    //private JsonArray data;
    //private int selectedFile = 0;

    private RecyclerView recyclerViewMusicList;
    private SwipeRefreshLayout swipeRefresh;
    private InternalMusicListAdapter internalAdapter;
    private ExternalMusicListAdapter externalAdapter;

    private final com.example.mediaplayer.CommonClasses.Handler Handler = new com.example.mediaplayer.CommonClasses.Handler();
    private int R_ID;
    private final MusicListInterface musicListInterface = ApiClient.getApiClient().create(MusicListInterface.class);

    private String selectedFileName;
    private ArrayList<String> arrayList;

    //private int lastExternalPlayPosition = -1;

    //private AudioManager audioManager;
    //private AudioManagerReceiver audioManagerReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        R_ID = R.id.activityMain_ImageView_Play;
        mediaPlayer = new MediaPlayer();
        mediaPlayer.stop();

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

        /*audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManagerReceiver = new AudioManagerReceiver();
        audioManagerReceiver.onReceive(this, getIntent());*/

        imageViewShuffleCheck.setVisibility(View.INVISIBLE);
        imageViewRepeatCheck.setVisibility(View.INVISIBLE);

        textViewOneOrALL.setVisibility(View.INVISIBLE);

        seekbar = findViewById(R.id.activityMain_SeekBar);
        seekbar.setClickable(false);

        imageViewPlay.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow));

        path = Objects.requireNonNull(getExternalFilesDir(Environment.DIRECTORY_MUSIC)).getPath();

        arrayList = new ArrayList<>();
        SetButtons();
        LoadPreferences();
        GetInternalMusicList();
        SetMusic(0);

        if (savedInstanceState != null) {
            internalAdapter.setPaused(savedInstanceState.getBoolean("isPaused"));
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isPaused", internalAdapter.isPaused());
    }

    @Override
    protected void onDestroy() {
        //audioManager.unregisterMediaButtonEventReceiver(audioManagerReceiver);
        super.onDestroy();
        //unregisterReceiver(onDownloadComplete);
    }

    private void SetButtons(){

        imageViewPlay.setOnClickListener(v -> {

            if(internalAdapter.getItemCount() < 1){
                return;
            }

            if(!internalAdapter.isPaused()){
                SetPlay(false);
                return;
            }

            SetPlay(true);
        });

        imageViewNext.setOnClickListener(v -> Next());

        imageViewPrevious.setOnClickListener(v -> {
            if(internalAdapter.getItemCount() < 1){
                return;
            }
            mediaPlayer.stop();

            if(currentTime > 5000){
                if(!internalAdapter.isPaused()) {
                    SetMusic(0);
                    SetPlay(true);
                }
                return;
            }

            if(internalAdapter.getFilePosition(selectedFileName) <= 0) {
                internalAdapter.setSelectedFileName(
                        internalAdapter.getFile(
                                internalAdapter.getItemCount()
                        ).getName()
                );

                //internalAdapter.setSelectedFileByPosition(internalAdapter.getItemCount() - 1);
                SetMusic(0);
            }else{
                SetMusic(-1);
            }

            if(!internalAdapter.isPaused()) {
                SetPlay(true);
            }
        });

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
            Handler.ShowSnack("Indisponível", "Função indisponível, pois causa problemas pra identificar qual musica está sendo executada na playlist", this, R_ID);
            //Shuffle();
            //SavePreferences();
        });

        imageViewInternalMusicList.setOnClickListener(v -> {
            DialogInternalMusicList();
        });

        imageViewExternalMusicList.setOnClickListener(v -> {
            GetInternalMusicList();
            GetExternalMusicList();
        });

        imageViewFavorite.setOnClickListener(v -> {
            Handler.ShowSnack("Implementando...", "Cria uma lista local na aplicação, mas ainda não pode ser executada", this, R_ID);
            setFavorite();
        });

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

    private void SetRecyclerView(boolean isExternal){
        try {
            recyclerViewMusicList.addOnItemTouchListener(new RecyclerItemClickListener(
                    this, recyclerViewMusicList, new RecyclerItemClickListener.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {

                    if(!isExternal) {
                        mediaPlayer.stop();
                        selectedFileName = internalAdapter.getFile(position).getName();
                        internalAdapter.setSelectedFileName(selectedFileName);
                        SetMusic(0);
                        SetPlay(true);
                    }else{
                        if(externalAdapter.getFileName(position).equals(externalAdapter.getSelectedFileName())) return;

                        File file = new File(
                                Objects.requireNonNull(getExternalFilesDir(Environment.DIRECTORY_MUSIC)).getAbsolutePath(),
                                externalAdapter.getDataInfo(position).getAsJsonObject().get("filename").getAsString()
                        );

                        if(file.exists()) {
                            /*int newPosition = -1;
                            for (int i = 0; i < internalAdapter.getItemCount(); i++) {
                                String filename = internalAdapter.getFile(i).getName();
                                if(filename.equalsIgnoreCase(file.getName())){
                                    newPosition = i;
                                    i = internalAdapter.getItemCount();
                                }
                            }
                            if(newPosition == -1) return;*/
                            mediaPlayer.stop();

                            selectedFileName = file.getName();
                            internalAdapter.setSelectedFileName(selectedFileName);
                            externalAdapter.setSelectedFileName(selectedFileName);

                            /*internalAdapter.setSelectedFileName(internalAdapter.getFile(newPosition).getName());
                            externalAdapter.setSelectedFileName(externalAdapter.getFileName(position));*/

                            SetMusic(0);
                            SetPlay(true);
                            return;
                        }

                        if(externalAdapter == null) return;
                        StreamPlay(externalAdapter.getFileName(position));
                        SetPlay(true);
                        internalAdapter.setPaused(true);
                        GetExternalMusicInfo(position);
                        externalAdapter.setSelectedFileName(externalAdapter.getFileName(position));
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
        int width = displayMetrics.widthPixels;
        searchView.setMaxWidth(width-150);

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
            internalAdapter.setSelectedFileName(selectedFileName);
            internalAdapter.setPaused(!mediaPlayer.isPlaying());

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
            Bitmap songImage = BitmapFactory.decodeByteArray(art, 0, art.length);
            imageViewArt.setImageBitmap(songImage);
            textViewArtistName.setText(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
            textViewMusicName.setText(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
        } catch (Exception e) {
            imageViewArt.setImageBitmap(null);
            String unknown = "Arquivo sem informações";
            String filename = source.replace(path,"").replace("/","").replace(".mp3","").replace(" - ","\n");
            textViewArtistName.setText(filename);
            textViewMusicName.setText(unknown);
        }
    }

    private void GetExternalMusicInfo(int position){
        JsonObject jsonObject = externalAdapter.getDataInfo(position);
        //imageViewArt.setImageBitmap(songImage);
        textViewArtistName.setText(jsonObject.get("artist").getAsString());
        textViewMusicName.setText(jsonObject.get("title").getAsString());
        imageViewArt.setImageDrawable(getResources().getDrawable(R.drawable.download));

        if(jsonObject.has("art")){
            imageViewArt.setImageBitmap(Handler.ImageDecode(jsonObject.get("art").getAsString()));
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

            internalAdapter.setSelectedFileName(selectedFileName);
            //internalAdapter.setSelectedFileName(internalAdapter.getFile(newPosition).getName());

            if (internalAdapter.getFile(newPosition).exists()) {
                mediaPlayer = MediaPlayer.create(this, Uri.parse(internalAdapter.getFile(newPosition).getAbsolutePath()));
                GetMusicInfo(internalAdapter.getFile(newPosition).getAbsolutePath());

                myHandler.postDelayed(UpdateSongTime,100);
            }

            SavePreferences();
            getFavorite();
        }catch (Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void Next(){
        if(internalAdapter.getItemCount() < 1){
            return;
        }
        mediaPlayer.stop();

        if(internalAdapter.getFilePosition(selectedFileName) >= internalAdapter.getItemCount()-1){
            selectedFileName = internalAdapter.getFile(0).getName();
            //internalAdapter.setSelectedFileName(selectedFileName);
            SetMusic(0);
        }else {
            SetMusic(1);
        }
        if(!internalAdapter.isPaused()) {
            SetPlay(true);
        }

        /*if(adapter != null) {
            adapter.setSelectedFile(selectedFile);
            adapter.notifyDataSetChanged();
        }*/
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

            RecyclerView.LayoutManager layoutManager;
            recyclerViewMusicList.setHasFixedSize(true);
            layoutManager = new LinearLayoutManager(this);
            recyclerViewMusicList.setLayoutManager(layoutManager);

            swipeRefresh.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.colorAccent));
            swipeRefresh.setColorSchemeColors(getResources().getColor(R.color.colorBlack));
            swipeRefresh.setOnRefreshListener(this::SwipeRefreshAction);

            recyclerViewMusicList.setAdapter(internalAdapter);

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
            externalAdapter.setSelectedFileName(selectedFileName);

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
        boolean isPause = internalAdapter.isPaused();
        GetInternalMusicList();
        Shuffle();
        internalAdapter.setSelectedFileName(selectedFileName);
        internalAdapter.setPaused(isPause);
        recyclerViewMusicList.setAdapter(internalAdapter);
        swipeRefresh.setRefreshing(false);
    }

    private void SetPlay(boolean isPlay){
        if(isPlay){
            imageViewPlay.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));
            mediaPlayer.start();
            internalAdapter.setPaused(false);
        }else{
            imageViewPlay.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow));
            mediaPlayer.pause();
            internalAdapter.setPaused(true);
        }
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
            mediaPlayer.start();
        } catch (Exception e) {
            Handler.ShowSnack("Houve um erro","MainActivity.StreamPlay: " + e.getMessage(), MainActivity.this, R_ID);
        }

        //String url = ApiClient.BASE_URL+song;

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

        if(shuffle){
            imageViewShuffleCheck.setVisibility(View.VISIBLE);
        }

        selectedFileName = settings.getString("selectedFileName","");
    }

    private void getFavorite(){
        boolean isFavorite = false;
        for (int i = 0; i < arrayList.size(); i++) {
            if(arrayList.get(i).equals(selectedFileName)){
                isFavorite = true;
            }
        }

        if(!isFavorite){
            imageViewFavorite.setImageDrawable(getResources().getDrawable(R.drawable.ic_favorite_border));
        }else{
            imageViewFavorite.setImageDrawable(getResources().getDrawable(R.drawable.ic_favorite));
        }
    }

    private void setFavorite(){
        boolean isFavorite = false;
        for (int i = 0; i < arrayList.size(); i++) {
            if(arrayList.get(i).equals(selectedFileName)){
                isFavorite = true;
            }
        }

        if(isFavorite){
            arrayList.remove(selectedFileName);
            imageViewFavorite.setImageDrawable(getResources().getDrawable(R.drawable.ic_favorite_border));
        }else{
            arrayList.add(selectedFileName);
            imageViewFavorite.setImageDrawable(getResources().getDrawable(R.drawable.ic_favorite));
        }
    }

    private final Runnable UpdateSongTime = new Runnable() {
        @SuppressLint("DefaultLocale")
        public void run() {
            currentTime = mediaPlayer.getCurrentPosition();
            finalTime = mediaPlayer.getDuration();

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

            Minutes = String.valueOf(TimeUnit.MILLISECONDS.toMinutes((long) finalTime));
            Seconds = String.valueOf(TimeUnit.MILLISECONDS.toSeconds((long) finalTime)
                    - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) finalTime)));
            if(Seconds.length() < 2){
                Seconds = "0"+Seconds;
            }
            textViewTotalTime.setText(String.format("%s:%s", Minutes, Seconds));

            //if (oneTimeOnly == 0) {
            seekbar.setMax((int) finalTime);
            //oneTimeOnly = 1;
            //}
            seekbar.setProgress((int)currentTime);

            if(!mediaPlayer.isPlaying()){
                //ResourcesCompat.getDrawable(Resources.getSystem(),R.drawable.ic_play_arrow, getTheme());
                imageViewPlay.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow));
            }

            myHandler.postDelayed(this, 100);
        }
    };

}