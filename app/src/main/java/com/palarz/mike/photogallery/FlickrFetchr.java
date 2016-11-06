package com.palarz.mike.photogallery;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mike on 11/2/16.
 */

public class FlickrFetchr {
    private static final String TAG = "FlickrFetchr";
    private static final String API_KEY = "9825d3f3846c7645fcb3d54aa87317cd";

    public byte[] getURLBytes(String URLSpec) throws IOException {
        URL url = new URL(URLSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                throw new IOException(connection.getResponseMessage() + ": with " + URLSpec);
            }

            int bytesRead = 0;
            byte [] buffer = new byte[1024];
            while((bytesRead = in.read(buffer)) > 0){
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        }
        finally{
            connection.disconnect();
        }
    }

    public String getURLString(String URLSpec) throws IOException{
        return new String(getURLBytes(URLSpec));
    }

    public List<GalleryItem> fetchItems(){
        List<GalleryItem> items = new ArrayList<>();

        try{
            String url = Uri.parse("https://api.flickr.com/services/rest/")
                    .buildUpon()
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    .appendQueryParameter("extras", "url_s")
                    .build().toString();

            String jsonString = getURLString(url);
            Log.i(TAG, "Received JSON: " + jsonString);
            JSONObject jsonBody = new JSONObject(jsonString);
            parseItems(items, jsonBody);
        }
        catch (JSONException je){
            Log.e(TAG, "Failed to parse JSON", je);
        }
        catch (IOException ioe){
            Log.e(TAG, "Failed to fetch items", ioe);
        }
        return items;
    }

    private void parseItems(List<GalleryItem> items, JSONObject jsonBody)
            throws IOException, JSONException{

        JSONObject photosJSONObject = jsonBody.getJSONObject("photos");
        JSONArray photoJSONArray = photosJSONObject.getJSONArray("photo");

        for(int i = 0; i < photoJSONArray.length(); i++){
            JSONObject photoJSONObject = photoJSONArray.getJSONObject(i);

            GalleryItem item = new GalleryItem();
            item.setID(photoJSONObject.getString("id"));
            item.setCaption(photoJSONObject.getString("title"));
//            Log.i(TAG, "The current photo's ID: " + photoJSONObject.getString("id"));
//            Log.i(TAG, "The current photo's title: " + photoJSONObject.getString("title"));

            if(!photoJSONObject.has("url_s"))
                continue;

            item.setURL(photoJSONObject.getString("url_s"));
            items.add(item);
        }
    }
}
