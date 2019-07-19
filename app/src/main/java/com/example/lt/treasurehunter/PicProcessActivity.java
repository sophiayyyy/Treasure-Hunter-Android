package com.example.lt.treasurehunter;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.WebDetection;
import com.google.api.services.vision.v1.model.WebLabel;
import com.google.firebase.database.DatabaseReference;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PicProcessActivity extends AppCompatActivity {

    private static final String CLOUD_VISION_API_KEY = "AIzaSyAg7SogfBKlDIsmuwN1YaliRnh1oADU1pA";
    private ProgressBar imageUploadProgress;
    private ImageView wholeImageView;
    private ImageButton takePictureBtn;
    private Feature featureWeb, featureLabel,featureLogo, featureText;
    private String finalTags = "";
    private String imageBase64;
    private List<String> finalLabels = new ArrayList();

    //To get user information from firebase
    DatabaseReference databaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pic_process);

        imageUploadProgress = (ProgressBar) findViewById(R.id.imageProgress);
        wholeImageView = (ImageView) findViewById(R.id.wholeImageView);
        takePictureBtn = (ImageButton) findViewById(R.id.takePictureBtn);

        //Turn to camera page
        takePictureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PicProcessActivity.this,TakePicActivity.class);
                startActivity(intent);
            }
        });

        //Choose different kinds of features we want to requests to.
        //In this app, we choose features as WEB_DETECTION, LABEL_DETECTION, LOGO_DETECTION and TEXT_DETECTION
        featureWeb = new Feature();
        featureWeb.setType("WEB_DETECTION");
        featureWeb.setMaxResults(5);

        featureLabel = new Feature();
        featureLabel.setType("LABEL_DETECTION");
        featureLabel.setMaxResults(5);

        featureLogo = new Feature();
        featureLogo.setType("LOGO_DETECTION");

        featureText = new Feature();
        featureText.setType("TEXT_DETECTION");
        featureText.setMaxResults(5);

        //Get images from image cropping section
        SharedPreferences shre = PreferenceManager.getDefaultSharedPreferences(this);
        String croppedImageBase64 = shre.getString("croppedImage", "");
        imageBase64 = shre.getString("recImage", "");
        Bundle bundle = getIntent().getExtras();

        Bitmap croppedBitmap = stringToBitMap(croppedImageBase64);
        Bitmap wholeImage = stringToBitMap(imageBase64);
        //Display the wholeImage
        wholeImageView.setImageBitmap(wholeImage);

        //Call Google Cloud Vision API for image recognition
        awesomeVision(croppedBitmap,featureWeb,featureLabel,featureLogo,featureText);
    }

    private void awesomeVision(final Bitmap bitmap, final Feature featureWeb, final Feature featureLabel,
                               final Feature featureLogo, final Feature featureText) {
        //Set progress bar visible
        imageUploadProgress.setVisibility(View.VISIBLE);
        //Add detected features to feature list
        final List<Feature> featureList = new ArrayList<>();
        featureList.add(featureWeb);
        featureList.add(featureLabel);
        featureList.add(featureLogo);
        featureList.add(featureText);

        final AnnotateImageRequest annotateImageReq = new AnnotateImageRequest();
        annotateImageReq.setFeatures(featureList);
        annotateImageReq.setImage(getImageEncodeImage(bitmap));

        // Create new thread
        new AsyncTask<Object, Void, String>() {
            @Override
            protected String doInBackground(Object... params) {
                try {
                    //Configuring the API Client
                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
                    VisionRequestInitializer requestInitializer = new VisionRequestInitializer(CLOUD_VISION_API_KEY);
                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(requestInitializer);
                    Vision vision = builder.build();
                    //make a request
                    BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(Arrays.asList(annotateImageReq));
                    Vision.Images.Annotate annotateRequest = vision.images().annotate(batchAnnotateImagesRequest);
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    AnnotateImageResponse imageResponses = response.getResponses().get(0);
                    //Get API results
                    WebDetection web;
                    web = imageResponses.getWebDetection();
                    List<WebLabel>wl = web.getBestGuessLabels();
                    List<EntityAnnotation> entityAnnotations;
                    entityAnnotations = imageResponses.getLabelAnnotations();
                    List<EntityAnnotation> entityAnnotationLogo;
                    entityAnnotationLogo = imageResponses.getLogoAnnotations();
                    List<EntityAnnotation> entityAnnotationText;
                    entityAnnotationText = imageResponses.getTextAnnotations();

                    //Tag filter: remove repeated tags, remove "null" and tags longest match principle
                    String finalString = "";
                    if (web != null) {
                        if(wl.get(0).getLabel()!=null){
                            Log.i("labels",wl.get(0).getLabel());
                            finalString += wl.get(0).getLabel();
                            finalLabels.add(wl.get(0).getLabel());}
                    }
                    if (entityAnnotations != null) {
                        String labelString = "";
                        for (EntityAnnotation entity : entityAnnotations) {
                            String labelEntity = entity.getDescription();
                            if(entity.getScore() > 0.9
                                    && (!labelEntity.equals("null"))
                                    && (!labelString.replace("-", " ").toLowerCase().
                                    contains(entity.getDescription().replace("-", " ")
                                            .toLowerCase()))){
                                labelEntity = labelEntity.replace(" null","");
                                labelEntity = labelEntity.replace("null ","");
                                if(!finalString.toLowerCase().replace("-", " ")
                                        .contains(labelEntity.replace("-", " ").toLowerCase())){
                                    finalString += labelEntity;
                                    finalLabels.add(labelEntity);
                                }
                            }
                        }
                    }
                    if (entityAnnotationLogo != null) {
                        if(!finalString.replace("-", " ").toLowerCase()
                                .contains(entityAnnotationLogo.get(0).getDescription()
                                        .replace("-", " ").toLowerCase())){
                            finalString += entityAnnotationLogo.get(0).getDescription();
                            finalLabels.add(entityAnnotationLogo.get(0).getDescription());
                        }
                    }
                    if (entityAnnotationText != null) {
                        int i = 0;
                        for (EntityAnnotation entity : entityAnnotationText) {
                            Log.d("search text", entity.getDescription());
                            String textEntity = entity.getDescription();
                            if(!textEntity.equals("null") && i < 4){
                                textEntity = textEntity.replace(" null","");
                                textEntity = textEntity.replace("null ","");
                                textEntity = textEntity.replace("\n","");
                                if(!finalString.replace("-", " ").toLowerCase().
                                        contains(textEntity.replace("-", " ").toLowerCase())
                                        && textEntity.length() < 30 && textEntity.length() > 2){
                                    finalString += textEntity;
                                    finalLabels.add(textEntity);
                                    i += 1;
                                }
                            }
                        }
                    }

                    finalTags = TextUtils.join(" ", finalLabels);
                    Log.i("labels",finalTags);
                    return "succeeded to make API request";
                } catch (GoogleJsonResponseException e) {
                    Log.d("picProcess", "failed to make API request because " + e.getContent());
                } catch (IOException e) {
                    Log.d("picProcess", "failed to make API request because of other IOException " + e.getMessage());
                }
                return "Cloud Vision API request failed. Check logs for details.";
            }

            protected void onPostExecute(String message) {
                imageUploadProgress.setVisibility(View.INVISIBLE);

                Intent i = new Intent(getApplicationContext(), BeforePostActivity.class );
                i.putExtra("labels",finalTags);
                startActivity(i);
                finish();
            }
        }.execute();
    }

    @NonNull
    private Image getImageEncodeImage(Bitmap bitmap) {
        Image base64EncodedImage = new Image();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        base64EncodedImage.encodeContent(imageBytes);
        return base64EncodedImage;
    }
    public Bitmap stringToBitMap(String encodedString) {
        try {
            byte[] encodeByte = Base64.decode(encodedString, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        } catch (Exception e) {
            e.getMessage();
            return null;
        }
    }
}
