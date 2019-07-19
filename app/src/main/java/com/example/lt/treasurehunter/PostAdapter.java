package com.example.lt.treasurehunter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private List<Post> postList;
    Context context;
    public boolean isShow;

    //listeners
    private OnRecyclerViewItemClickListener mOnItemClickListener = null;
    private OnRecyclerViewItemCheckedListener mOnItemCheckedListener = null;
    public void setOnItemClickListener(OnRecyclerViewItemClickListener listener){
        this.mOnItemClickListener = listener;
    }
    public void setOnItemCheckedListener(OnRecyclerViewItemCheckedListener listener){
        this.mOnItemCheckedListener = listener;
    }
    //enum used to check item itself or button
    public enum  ViewName{
        ITEM,
        LIKE
    }

    //item itself click listener
    public interface OnRecyclerViewItemClickListener {
        void onClick(View view, ViewName viewName, int position);

    }

    //toggleButton checked listener
    public interface OnRecyclerViewItemCheckedListener{
        void onCheckedChanged(CompoundButton buttonView, ViewName viewName,boolean isChecked, int position);
    }

    public void changeShow(boolean isShow){
        this.isShow = isShow;
    }

    public PostAdapter(Context context, List<Post> postList) {
        this.postList = postList;
        this.context = context;
    }

    @Override
    public void onClick(View v) {
        int position = (int)v.getTag();
        if(mOnItemClickListener!=null){
            mOnItemClickListener.onClick(v,ViewName.ITEM,position);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int position = (int) buttonView.getTag();
        if (mOnItemCheckedListener!=null){
            mOnItemCheckedListener.onCheckedChanged(buttonView, ViewName.LIKE,isChecked,position);
        }
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView img_post;
        private TextView txt_tag;
        private  TextView txt_like;
        private ToggleButton btn_like;

        public ViewHolder(View itemView) {
            super(itemView);
            img_post = (ImageView) itemView.findViewById(R.id.iv_photo);
            txt_tag = (TextView) itemView.findViewById(R.id.txt_tag);
            txt_like = (TextView) itemView.findViewById(R.id.txt_like);
            btn_like = (ToggleButton) itemView.findViewById(R.id.btn_like);
            //click
            itemView.setOnClickListener(PostAdapter.this);
            btn_like.setOnCheckedChangeListener(PostAdapter.this);

        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.post_row,parent,false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Post post = postList.get(position);
        byte[] encodeByte = Base64.decode(post.getPostImageBase64() ,Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
        String tag = post.getTag();
        String like = String.valueOf(post.getLike());

        holder.img_post.setImageBitmap(bitmap);
        holder.txt_tag.setText(tag);
        holder.txt_like.setText(like); //the value of setText cannot be int

        holder.itemView.setTag(position);
        holder.btn_like.setTag(position);


    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void updateItems(List<Post> newList) {
        postList.clear();
        postList.addAll(newList);
        this.notifyDataSetChanged();
    }

}
