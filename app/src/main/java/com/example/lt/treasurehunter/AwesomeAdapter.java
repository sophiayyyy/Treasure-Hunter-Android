package com.example.lt.treasurehunter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class AwesomeAdapter extends BaseAdapter {
    public ArrayList<HashMap<String, String>> arraylist ;
    final static String TAG = "searchTest";
    private int selectedPosition = -1;
    private Context context;
    private int layout_num; // 0 for shopping_items, 1 for bestOption
    public AwesomeAdapter(Context aContext, ArrayList<HashMap<String, String>> arraylist, int layout_num) {
        //initializing our data in the constructor.
        context = aContext;  //saving the context we'll need it again.
        this.arraylist = arraylist;
        this.layout_num = layout_num;
    }
    @Override
    public int getCount() {
        return arraylist.size();
    }

    @Override
    public Object getItem(int position) {
        return arraylist.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row;
        if (convertView == null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if(layout_num == 1) {
                row = inflater.inflate(R.layout.bestoption_listview_row, parent, false);
            }else {
                row = inflater.inflate(R.layout.shopping_item_listview_row, parent, false);
            }
        }
        else{
            row = convertView;
        }
        //initialize row views
        ImageView itemImage = (ImageView) row.findViewById(R.id.itemImage);
        TextView itemPrice = (TextView) row.findViewById(R.id.itemPrice);
        TextView itemSource = (TextView) row.findViewById(R.id.itemSource);
        TextView itemTitle = (TextView) row.findViewById(R.id.itemTitle);
        TextView itemLink = (TextView) row.findViewById(R.id.itemLink);
        CheckBox checkBox = (CheckBox) row.findViewById(R.id.checkBox);

        String imageurl = arraylist.get(position).get("thumbnail");
        if (imageurl ==""){
            //no thumbnail provided use the default treasure hunter image
            itemImage.setImageResource(R.drawable.treasure_hunter);
        }else if(imageurl.contains("data:image")){
            //decode base64 webp to bitmap
            String cleanBase64 = imageurl.replace("data:image/webp;base64,", "").replace("data:image/jpeg;base64,","");
            byte[] decodedString = Base64.decode(cleanBase64, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            Bitmap roundImage = roundImageHelper.getRoundedCornerBitmap(decodedByte,8);
            itemImage.setImageBitmap(roundImage);

        }else if (imageurl.contains("https://")){
            //download image based on the url
            new DownloadImageTask(itemImage).execute(imageurl);
        }
        itemPrice.setText(arraylist.get(position).get("price"));
        itemSource.setText(arraylist.get(position).get("source"));
        itemTitle.setText(arraylist.get(position).get("title"));
        itemLink.setText(arraylist.get(position).get("link"));
        if(arraylist.get(position).get("visible")=="false"){
            checkBox.setVisibility(View.GONE);
        }else if (arraylist.get(position).get("visible")=="true"){
            checkBox.setVisibility(View.VISIBLE);
        }
        if (position == selectedPosition) {
            checkBox.setChecked(true);
        } else {
            checkBox.setChecked(false);
        }
        checkBox.setOnClickListener(onStateChangedListener(checkBox, position));
        return row;
    }
    public void updateAdapter() {
        this.notifyDataSetChanged();
    }
    public int getSelectedPosition() {
        return selectedPosition;
    }
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            Bitmap roundImage = roundImageHelper.getRoundedCornerBitmap(mIcon11,8);

            return roundImage;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }
    private View.OnClickListener onStateChangedListener(final CheckBox checkBox, final int position) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkBox.isChecked()) {
                    selectedPosition = position;
                } else {
                    selectedPosition = -1;
                }
                notifyDataSetChanged();
            }
        };
    }
}
