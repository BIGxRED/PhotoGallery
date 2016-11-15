package com.palarz.mike.photogallery;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;

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

    public List<GalleryItem> fetchItems(Integer pageNumber){
        List<GalleryItem> items = new ArrayList<>();

        try{
            /*
            String url = Uri.parse("https://api.flickr.com/services/rest/")
                    .buildUpon()
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    .appendQueryParameter("extras", "url_s")
                    .build().toString();
            */
            String url = parseURI(pageNumber);
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
        Gson gson = new Gson();

        JSONObject photosJSONObject = jsonBody.getJSONObject("photos");
        JSONArray photoJSONArray = photosJSONObject.getJSONArray("photo");

        for(int i = 0; i < photoJSONArray.length(); i++){
            JSONObject photoJSONObject = photoJSONArray.getJSONObject(i);

            //Code for challenge
            GalleryItem item = gson.fromJson(photoJSONObject.toString(), GalleryItem.class);

            //If the current item doesn't have a value for "url_s", it will not be added to the
            //array of GalleryItems
            if(!photoJSONObject.has("url_s"))
                continue;

//            item.setURL(photoJSONObject.getString("url_s"));
            Log.i(TAG, "Current gallery item: " + item.toString());
            items.add(item);
        }
    }

    private String parseURI(int pageNumber){
        return Uri.parse("https://api.flickr.com/services/rest/")
                .buildUpon()
                .appendQueryParameter("method", "flickr.photos.getRecent")
                .appendQueryParameter("api_key", API_KEY)
                .appendQueryParameter("format", "json")
                .appendQueryParameter("nojsoncallback", "1")
                .appendQueryParameter("extras", "url_s")
                //Part of challenge: Pulls in a page specified by pageNumber for endless scrolling
                .appendQueryParameter("page", Integer.toString(pageNumber))
                .build().toString();
    }
}
