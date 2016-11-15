package com.palarz.mike.photogallery;

import com.google.gson.annotations.SerializedName;

/**
 * Created by mike on 11/5/16.
 */
public class GalleryItem {

    /**
     * The @SerealizedName() annotation is used so that the GSON parser can properly map the member
     * variable to the key value within the JSON data. This annotation was necessary since our
     * member variables do not have the same names as the JSON keys.
     */

    @SerializedName("title")
    private String mCaption;

    @SerializedName("id")
    private String mID;

    @SerializedName("url_s")
    private String mURL;

    public String getCaption() {
        return mCaption;
    }

    public void setCaption(String mCaption) {
        this.mCaption = mCaption;
    }

    public String getID() {
        return mID;
    }

    public void setID(String mID) {
        this.mID = mID;
    }

    public String getURL() {
        return mURL;
    }

    public void setURL(String mURL) {
        this.mURL = mURL;
    }

    @Override
    public String toString(){
        return mCaption;
    }
}
