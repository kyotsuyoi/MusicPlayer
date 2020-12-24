package com.example.mediaplayer;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mediaplayer.CommonClasses.ApiClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import pl.droidsonroids.gif.GifImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Context.DOWNLOAD_SERVICE;

public class ExternalMusicListAdapter extends RecyclerView.Adapter <ExternalMusicListAdapter.ViewHolder> {

    private final List<File> files;
    private final Activity activity;
    private final com.example.mediaplayer.CommonClasses.Handler Handler = new com.example.mediaplayer.CommonClasses.Handler();
    private final int R_ID;
    private final MusicListInterface musicListInterface = ApiClient.getApiClient().create(MusicListInterface.class);
    private boolean isBindViewHolderError;

    private final JsonArray data;
    private JsonArray filteredData;

    private String selectedFileName;

    private long downloadID;

    public ExternalMusicListAdapter(List<File> files, JsonArray data, Activity activity, int R_ID) {
        this.data = data;
        this.filteredData = data;
        this.files = files;
        this.activity = activity;
        this.R_ID = R_ID;
    }

    @NonNull
    public ViewHolder onCreateViewHolder(ViewGroup parent, int ViewType) {
        activity.registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_external_music_list,parent,false);
        return new ViewHolder(view);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        try {
            String filename = filteredData.get(position).getAsJsonObject().get("filename").getAsString();
            String title = filteredData.get(position).getAsJsonObject().get("title").getAsString();
            String artist = filteredData.get(position).getAsJsonObject().get("artist").getAsString();

            viewHolder.imageViewDownload.setVisibility(View.VISIBLE);
            if(files!=null) {
                for (int i = 0; i < files.size(); i++) {
                    String Name = files.get(i).getName();
                    if (Name.equalsIgnoreCase(filename)) {
                        viewHolder.imageViewDownload.setVisibility(View.INVISIBLE);
                        i = files.size();
                    }
                }
            }

            viewHolder.gifImageView.setVisibility(View.INVISIBLE);
            String fileName = filteredData.get(position).getAsJsonObject().get("filename").getAsString();
            if(selectedFileName.equals(fileName)){
                viewHolder.gifImageView.setVisibility(View.VISIBLE);
            }

            viewHolder.textViewArtistName.setText(artist);
            viewHolder.textViewMusicName.setText(title);

            GetMusicArt(viewHolder.imageViewArt, filename,position);

            viewHolder.imageViewDownload.setOnClickListener(v->{
                File file = new File(Objects.requireNonNull(activity.getExternalFilesDir(Environment.DIRECTORY_MUSIC)).getAbsolutePath(),filename);
                if(!file.exists()) {
                    beginDownload(filename);
                }
            });

        }catch (Exception e){
            if(!isBindViewHolderError) {
                Handler.ShowSnack("Houve um erro", "ExternalMusicListAdapter.onBindViewHolder: " + e.getMessage(), activity, R_ID);
                isBindViewHolderError=true;
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        ImageView imageViewArt, imageViewDownload;
        TextView textViewArtistName, textViewMusicName;
        GifImageView gifImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            imageViewArt = itemView.findViewById(R.id.itemExternalMusicList_ImageView_Art);
            textViewArtistName = itemView.findViewById(R.id.itemExternalMusicList_TextView_ArtistName);
            textViewMusicName = itemView.findViewById(R.id.itemExternalMusicList_TextView_MusicName);
            imageViewDownload = itemView.findViewById(R.id.itemExternalMusicList_ImageView_Download);
            gifImageView = itemView.findViewById(R.id.itemExternalMusicList_ImageView_Gif);
        }
    }

    public int getItemCount() {
        if(filteredData==null) {
            return files.size();
        }
        return filteredData.size();
    }

    public JsonObject getDataInfo(int position){
        return filteredData.get(position).getAsJsonObject();
    }

    private void GetMusicArt(ImageView imageView, String filename, int position){
        try {
            if(filteredData.get(position).getAsJsonObject().has("art")){
                imageView.setImageBitmap(Handler.ImageDecode(filteredData.get(position).getAsJsonObject().get("art").getAsString()));
                return;
            }

            Call<JsonObject> call = musicListInterface.GetMusicArt(filename);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    try {
                        if (!Handler.isRequestError(response, activity, R_ID)){
                            JsonObject jsonObject = response.body();
                            assert jsonObject != null;
                            JsonArray jsonArray = jsonObject.get("data").getAsJsonArray();

                            imageView.setImageBitmap(Handler.ImageDecode(jsonArray.get(0).getAsJsonObject().get("art").getAsString()));

                            JsonObject newJsonObject = data.get(position).getAsJsonObject();
                            newJsonObject.addProperty("art",jsonArray.get(0).getAsJsonObject().get("art").getAsString());
                            data.set(position,newJsonObject);
                        }else{
                            imageView.setImageBitmap(null);
                        }
                    }catch (Exception e){
                        Handler.ShowSnack("Houve um erro","ExternalMusicListAdapter.GetMusicArt.onResponse: " + e.getMessage(), activity, R_ID);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                    Handler.ShowSnack("Houve um erro","ExternalMusicListAdapter.GetMusicArt.onFailure: " + t.toString(), activity, R_ID);
                }
            });

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","ExternalMusicListAdapter.GetMusicArt: " + e.getMessage(), activity, R_ID);
        }
    }

    public String getFileName(int position){
        return filteredData.get(position).getAsJsonObject().get("filename").getAsString();
    }

    public void setSelectedFileName(String filename){
        selectedFileName = filename;
        notifyDataSetChanged();
    }

    public String getSelectedFileName(){
        return selectedFileName;
    }

    public Filter getFilter() {
        return new Filter()
        {
            protected FilterResults performFiltering(CharSequence charSequence)
            {
                FilterResults results = new FilterResults();

                if(charSequence == null || charSequence.length() == 0){
                    results.values = data;
                    results.count = data.size();
                }else{

                    JsonArray jsonArray = new JsonArray();

                    int i = 0;
                    try {

                        while (i < data.size()) {
                            data.get(i).getAsJsonObject().get("filename");
                            String A = data.get(i).getAsJsonObject().get("filename").toString().toLowerCase();
                            String B = charSequence.toString().toLowerCase();
                            if (A.contains(B)) {
                                jsonArray.add(data.get(i));
                            }
                            i++;
                        }

                        results.values = jsonArray;
                        results.count = jsonArray.size();

                    }catch (Exception e){
                        //Handler.ShowSnack("Houve um erro", "MusicListAdapter.getFilter: " + e.getMessage(), activity, R_ID, true);
                    }
                }

                return results;
            }

            protected void publishResults(CharSequence charSequence, FilterResults filterResults)
            {
                filteredData = (JsonArray) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    /*public void findSelected(String fileName){
        for (int i = 0; i < filteredData.size(); i++) {
            JsonObject jsonObject = filteredData.get(i).getAsJsonObject();
            if(fileName.equalsIgnoreCase(jsonObject.get("filename").getAsString())){
                selectedFile = i;
            }
        }
    }*/

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void beginDownload(String filename){
        File file = new File(Objects.requireNonNull(activity.getExternalFilesDir(Environment.DIRECTORY_MUSIC)).getAbsolutePath(),filename);
        DownloadManager.Request request=new DownloadManager.Request(Uri.parse(ApiClient.BASE_URL+filename))
                .setTitle(filename)// Title of the Download Notification
                .setDescription("Baixando")// Description of the Download Notification
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)// Visibility of the download Notification
                .setDestinationUri(Uri.fromFile(file))// Uri of the destination file
                .setRequiresCharging(false)// Set if charging is required to begin the download
                .setAllowedOverMetered(true)// Set if download is allowed on Mobile network
                .setAllowedOverRoaming(true);// Set if download is allowed on roaming network
        DownloadManager downloadManager= (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
        downloadID = downloadManager.enqueue(request);// enqueue puts the download request in the queue.
    }

    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Fetching the download id received with the broadcast
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            //Checking if the received broadcast is for our enqueued download by matching download id
            if (downloadID == id) {
                GetInternalMusicList();
                notifyDataSetChanged();
            }
        }
    };

    public void GetInternalMusicList(){
        try {
            File dir = new File(Objects.requireNonNull(activity.getExternalFilesDir(Environment.DIRECTORY_MUSIC)).getAbsolutePath());

            files.clear();
            files.addAll(Arrays.asList(Objects.requireNonNull(dir.listFiles())));

            Collections.sort(files);
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.GetInternalMusicList: " + e.getMessage(), activity, R_ID);
        }
    }

}
