package com.RockLinker;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.List;

public class ExternalArtistListAdapter extends RecyclerView.Adapter <ExternalArtistListAdapter.ViewHolder> {

    private final Activity activity;
    private final com.RockLinker.CommonClasses.Handler Handler = new com.RockLinker.CommonClasses.Handler();
    private final int R_ID;
    private boolean isBindViewHolderError;

    private final JsonArray data;
    private JsonArray filteredData;

    public ExternalArtistListAdapter(JsonArray data, Activity activity, int R_ID) {
        this.data = data;
        this.filteredData = data;
        this.activity = activity;
        this.R_ID = R_ID;
    }

    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int ViewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_external_artist_list,parent,false);
        return new ViewHolder(view);
    }

    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        try {
            JsonObject jsonObject = filteredData.get(position).getAsJsonObject();

            String quantity = jsonObject.get("quantity").getAsString()+" músicas";
            viewHolder.textViewArtist.setText(jsonObject.get("artist").getAsString());
            viewHolder.textViewQuantity.setText(quantity);

            //viewHolder.imageViewArt.setImageBitmap(Handler.ImageDecode(jsonObject.get("art").getAsString()));

        }catch (Exception e){
            if(!isBindViewHolderError) {
                Handler.ShowSnack(
                        "Houve um erro",
                        "ExternalArtistListAdapter.onBindViewHolder: " + e.getMessage()+ " on position "+ position,
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
            imageViewArt = itemView.findViewById(R.id.itemExternalArtistList_ImageView_Art);
            textViewArtist = itemView.findViewById(R.id.itemExternalArtistList_TextView_ArtistName);
            textViewQuantity = itemView.findViewById(R.id.itemExternalArtistList_TextView_Quantity);
        }
    }

    public int getItemCount() {
        return filteredData.size();
    }

    public String getArtistName(int position){
        return filteredData.get(position).getAsJsonObject().get("artist").getAsString();
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
                            String A = data.get(i).getAsJsonObject().get("artist").toString().toLowerCase();
                            String B = charSequence.toString().toLowerCase();
                            if (A.contains(B)) {
                                jsonArray.add(data.get(i));
                            }
                            i++;
                        }

                        results.values = jsonArray;
                        results.count = jsonArray.size();

                    }catch (Exception e){
                        Toast.makeText(activity,e.getMessage(),Toast.LENGTH_LONG).show();
                        results.values = data;
                        results.count = data.size();
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

}
