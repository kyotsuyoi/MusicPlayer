package com.example.mediaplayer;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import pl.droidsonroids.gif.GifImageView;

public class InternalMusicListAdapter extends RecyclerView.Adapter <InternalMusicListAdapter.ViewHolder> {

    private final List<File> files;
    private List<File> filteredFiles;
    private final Activity activity;
    private final com.example.mediaplayer.CommonClasses.Handler Handler = new com.example.mediaplayer.CommonClasses.Handler();
    private final int R_ID;
    private boolean isBindViewHolderError;

    private String selectedFileName;
    private boolean isPaused = true;

    public InternalMusicListAdapter(List<File> files, Activity activity, int R_ID) {
        this.files = files;
        this.filteredFiles = files;
        this.activity = activity;
        this.R_ID = R_ID;
    }

    @NonNull
    public ViewHolder onCreateViewHolder(ViewGroup parent, int ViewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_internal_music_list,parent,false);
        return new ViewHolder(view);
    }

    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        try {
            GetMusicInfo(
                    filteredFiles.get(position).getAbsolutePath(),
                    viewHolder.imageViewArt,
                    viewHolder.textViewArtistName,
                    viewHolder.textViewMusicName
            );

            String fileName = filteredFiles.get(position).getName();
            if (selectedFileName.equals(fileName)){
                viewHolder.imageViewPlaying.setVisibility(View.VISIBLE);
                if (isPaused) {
                    viewHolder.imageViewPlaying.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_pause));
                    viewHolder.gifImageView.setVisibility(View.INVISIBLE);
                } else {
                    viewHolder.imageViewPlaying.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_play_arrow));
                    viewHolder.gifImageView.setVisibility(View.VISIBLE);
                }
            } else {
                viewHolder.imageViewPlaying.setVisibility(View.INVISIBLE);
                viewHolder.gifImageView.setVisibility(View.INVISIBLE);
            }

        }catch (Exception e){
            if(!isBindViewHolderError) {
                Handler.ShowSnack("Houve um erro", "MusicListAdapter.onBindViewHolder: " + e.getMessage(), activity, R_ID);
                isBindViewHolderError=true;
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        ImageView imageViewArt, imageViewPlaying;
        TextView textViewArtistName, textViewMusicName;
        GifImageView gifImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            imageViewArt = itemView.findViewById(R.id.itemInternalMusicList_ImageView_Art);
            textViewArtistName = itemView.findViewById(R.id.itemInternalMusicList_TextView_ArtistName);
            textViewMusicName = itemView.findViewById(R.id.itemInternalMusicList_TextView_MusicName);
            imageViewPlaying = itemView.findViewById(R.id.itemInternalMusicList_ImageView_Playing);
            gifImageView = itemView.findViewById(R.id.itemInternalMusicList_ImageView_Gif);
        }
    }

    public int getItemCount() {
        return filteredFiles.size();
    }

    public File getFile(int position){
        return filteredFiles.get(position);
    }

    public List<File> getFiles(){
        return files;
    }

    public boolean isPaused(){
        return isPaused;
    }

    public void setPaused(boolean isPaused){
        this.isPaused = isPaused;
    }

    public void setShuffle(boolean isShuffle){
        if(isShuffle){
            Collections.shuffle(filteredFiles);
        }else{
            Collections.sort(filteredFiles);
        }
    }

    /*public void setSelectedFileByPosition(int position){
        selectedFileName = filteredFiles.get(position);
        notifyDataSetChanged();
    }*/

    /*public void setSelectedFile(File file){
        selectedFile = file;
    }*/

    /*public String getSelectedFile(){
        return selectedFileName;
    }*/

    public int getFilePosition(String fileName){
        for (int i = 0; i < filteredFiles.size(); i++) {
            if(fileName.equals(filteredFiles.get(i).getName())) return i;
        }
        return -1;
    }

    /*public void setSelectedFileByName(String fileName){
        for (File file:files){
            if(file.getName().equals(fileName)){
                selectedFile = file;
            }
        }
    }*/

    public void setSelectedFileName(String fileName){
        selectedFileName = fileName;
        notifyDataSetChanged();
    }

    public Filter getFilter() {
        return new Filter()
        {
            protected FilterResults performFiltering(CharSequence charSequence)
            {
                FilterResults results = new FilterResults();

                if(charSequence == null || charSequence.length() == 0){
                    results.values = files;
                    results.count = files.size();
                }else{

                    List<File> newFiles = new ArrayList<>();

                    int i = 0;
                    try {

                        while (i < files.size()) {
                            files.get(i).getName().toLowerCase();
                            String A = files.get(i).getName().toLowerCase();
                            String B = charSequence.toString().toLowerCase();
                            if (A.contains(B)) {
                                newFiles.add(files.get(i));
                            }
                            i++;
                        }

                        results.values = newFiles;
                        results.count = newFiles.size();

                    }catch (Exception e){
                        //Handler.ShowSnack("Houve um erro", "MusicListAdapter.getFilter: " + e.getMessage(), activity, R_ID, true);
                    }
                }

                return results;
            }

            protected void publishResults(CharSequence charSequence, FilterResults filterResults)
            {
                filteredFiles = (List<File>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    private void GetMusicInfo(String source, ImageView imageViewArt, TextView textViewArtistName, TextView textViewMusicName){
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(source);

        String path = Objects.requireNonNull(activity.getExternalFilesDir(Environment.DIRECTORY_MUSIC)).getPath();

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
            String filename = source.replace(path,"").replace("/","").replace(".mp3","");
            textViewArtistName.setText(filename);
            textViewMusicName.setText(unknown);
        }
    }

}
