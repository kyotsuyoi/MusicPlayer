package com.RockLinker;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.RockLinker.R;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import pl.droidsonroids.gif.GifImageView;

public class InternalArtistListAdapter extends RecyclerView.Adapter <InternalArtistListAdapter.ViewHolder> {

    private JsonArray list;
    private JsonArray filteredList;
    private final Activity activity;
    private final com.RockLinker.CommonClasses.Handler Handler = new com.RockLinker.CommonClasses.Handler();
    private final int R_ID;
    private boolean isBindViewHolderError;

    public InternalArtistListAdapter(JsonArray list, Activity activity, int R_ID) {
        this.list = list;
        this.filteredList = list;
        this.activity = activity;
        this.R_ID = R_ID;
    }

    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int ViewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_internal_artist_list,parent,false);
        return new ViewHolder(view);
    }

    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        try {
            JsonObject jsonObject = filteredList.get(position).getAsJsonObject();

            String quantity = jsonObject.get("quantity").getAsString()+" músicas";
            viewHolder.textViewArtist.setText(jsonObject.get("artist").getAsString());
            viewHolder.textViewQuantity.setText(quantity);

            viewHolder.imageViewArt.setImageBitmap(Handler.ImageDecode(jsonObject.get("art").getAsString()));

        }catch (Exception e){
            if(!isBindViewHolderError) {
                Handler.ShowSnack(
                        "Houve um erro",
                        "InternalArtistListAdapter.onBindViewHolder: " + e.getMessage(),
                        activity,
                        R_ID
                );
                isBindViewHolderError=true;
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        ImageView imageViewArt;
        TextView textViewArtist, textViewQuantity;

        public ViewHolder(View itemView) {
            super(itemView);
            imageViewArt = itemView.findViewById(R.id.itemInternalArtistList_ImageView_Art);
            textViewArtist = itemView.findViewById(R.id.itemInternalArtistList_TextView_ArtistName);
            textViewQuantity = itemView.findViewById(R.id.itemInternalArtistList_TextView_Quantity);
        }
    }

    public int getItemCount() {
        return filteredList.size();
    }

    public String getArtistName(int position){
        return filteredList.get(position).getAsJsonObject().get("artist").getAsString();
    }

    /*public List<File> getFiles(){
        return files;
    }

    public void setShuffle(boolean isShuffle){
        if(isShuffle){
            Collections.shuffle(filteredFiles);
        }else{
            Collections.sort(filteredFiles);
        }
    }

    public int getFilePosition(String fileName){
        for (int i = 0; i < filteredFiles.size(); i++) {
            if(fileName.equals(filteredFiles.get(i).getName())) return i;
        }
        return -1;
    }

    public Filter getFilter()
    {
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
                        //Handler.ShowSnack("Houve um erro",
                        // "MusicListAdapter.getFilter: " + e.getMessage(), activity, R_ID, true);
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
    }*/

    /*private void GetMusicInfo(String source, ImageView imageViewArt, TextView textViewArtist){
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(source);

        String path = Objects.requireNonNull(activity.getExternalFilesDir(Environment.DIRECTORY_MUSIC)).getPath();

        try {
            byte[] art = mediaMetadataRetriever.getEmbeddedPicture();
            assert art != null;
            Bitmap songImage = BitmapFactory.decodeByteArray(art, 0, art.length);
            imageViewArt.setImageBitmap(songImage);

            textViewArtist.setText(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
        } catch (Exception e) {
            imageViewArt.setImageBitmap(null);
            String unknown = "Arquivo sem informações";
            String filename = source.replace(path,"").replace("/","").replace(".mp3","");
            textViewArtist.setText(filename);
        }
    }*/

}
