package com.palarz.mike.photogallery;

/**
 * Created by mike on 11/5/16.
 */
public class GalleryItem {
    private String mCaption;
    private String mID;
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
