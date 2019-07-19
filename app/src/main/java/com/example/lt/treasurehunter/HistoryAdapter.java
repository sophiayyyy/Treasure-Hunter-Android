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

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<Post> historyList;
    Context context;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(int position);

    }

    public void setOnItemClickListener(OnItemClickListener listener){
        mListener = listener;
    }

    public HistoryAdapter(Context context, List<Post> historyList) {
        this.historyList = historyList;
        this.context = context;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView iv_history_1;
        private TextView txt_history_tag_1;


        public ViewHolder(View itemView) {
            super(itemView);
            iv_history_1 = (ImageView) itemView.findViewById(R.id.iv_history);
            txt_history_tag_1 = (TextView) itemView.findViewById(R.id.txt_history_tag);

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
    public HistoryAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_row,parent,false);
        HistoryAdapter.ViewHolder holder = new HistoryAdapter.ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        Post post = historyList.get(i);
        byte[] encodeByte = Base64.decode(post.getPostImageBase64() ,Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
        String tag = post.getTag();
        String like = String.valueOf(post.getLike());

        viewHolder.iv_history_1.setImageBitmap(bitmap);
        viewHolder.txt_history_tag_1.setText(tag);

    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }
}
