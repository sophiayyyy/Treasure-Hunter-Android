package com.example.lt.treasurehunter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {

    private List<Post> favoritesList;
    Context context;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(int position);

    }

    public void setOnItemClickListener(OnItemClickListener listener){
        mListener = listener;
    }

    public FavoritesAdapter(Context context, List<Post> favoritesList) {
        this.favoritesList = favoritesList;
        this.context = context;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView iv_favorites;
        private TextView txt_favorites_tag;
        //private TextView txt_favorites_like_number;

        public ViewHolder(View itemView) {
            super(itemView);
            iv_favorites = (ImageView) itemView.findViewById(R.id.iv_favorites);
            txt_favorites_tag = (TextView) itemView.findViewById(R.id.txt_favorites_tag);
            //txt_favorites_like_number = (TextView) itemView.findViewById(R.id.txt_favorites_like_number);

//            txt_favorites_like_number.setVisibility(View.INVISIBLE);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mListener != null){
                        int position = getAdapterPosition();
                        if(position != RecyclerView.NO_POSITION){
                            mListener.onItemClick(position);
                        }
                    }
                }
            });
        }
    }

    @Override
    public FavoritesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.favorites_row,parent,false);
        FavoritesAdapter.ViewHolder holder = new FavoritesAdapter.ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        Post post = favoritesList.get(i);
        byte[] encodeByte = Base64.decode(post.getPostImageBase64() ,Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
        String tag = post.getTag();
        String like = String.valueOf(post.getLike());

        viewHolder.iv_favorites.setImageBitmap(bitmap);
        viewHolder.txt_favorites_tag.setText(tag);
        //viewHolder.txt_favorites_like_number.setText(like); //the value of setText cannot be int

    }

    @Override
    public int getItemCount() {
        return favoritesList.size();
    }
}
