package com.example.lt.treasurehunter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Post implements Serializable {
    private String postId;
    private String tag;
    private int like;
    private int imgID;
    private String postImageBase64;
    public String price;
    public String source;
    public String title;
    public String link;
    public String thumbnail;

    public Post(String postId, String tag, int imgID, int like,
                String postImageBase64, String price, String source, String title, String link, String thumbnail) {
        this.postId = postId;
        this.tag = tag;
        this.like = like;
        this.imgID = imgID;
        this.postImageBase64 = postImageBase64;
        this.price = price;
        this.source = source;
        this.title = title;
        this.link = link;
        this.thumbnail = thumbnail;
    }
    public Post(){

    }

    public String getPostId(){
        return postId;
    }

    public void setPostId(String postId){
        this.postId = postId;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getLike(){
        return like;
    }

    public  void setLike(int like){
        this.like = like;
    }

    public int getImgID() {
        return imgID;
    }

    public void setImgID(int imgID) {
        this.imgID = imgID;
    }

    public String getPostImageBase64() {
        return postImageBase64;
    }

    public void setPostImageBase64(String postImageBase64){
        this.postImageBase64 = postImageBase64;
    }

    public String getPrice(){
        return price;
    }

    public void setPrice(String price){
        this.price = price;
    }

    public String getSource(){
        return source;
    }

    public void setSource(String source){
        this.source = source;
    }

    public String getTitle(){
        return title;
    }

    public void setTitle(String title){
        this.title= title;
    }

    public String getLink(){
        return link;
    }

    public void setLink(String link){
        this.link = link;
    }

    public String getThumbnail(){
        return thumbnail;
    }

    public void setThumbnail(String thumbnail){
        this.thumbnail = thumbnail;
    }
    //construct the hashMap that contains all the information of the best matching item
    public HashMap<String, String> getBestOption() {
        HashMap<String, String> bestOp = new HashMap<>();
        bestOp.put("postId", postId);
        bestOp.put("price", price);
        bestOp.put("source", source);
        bestOp.put("title", title);
        bestOp.put("link", link);
        bestOp.put("thumbnail", thumbnail);
        return bestOp;
    }

}
