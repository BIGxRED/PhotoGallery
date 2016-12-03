package com.palarz.mike.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by mike on 11/15/16.
 */
public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";

    //Used to identify messages as download requests
    private static final int MESSAGE_DOWNLOAD = 0;
    private final static int MESSAGE_PRELOAD = 1;

    //The message handler; responsible for queueing message requests onto the ThumbnailDownloader
    //background thread; also responsible for processing download request messages when they are
    // pulled off the queue
    private Handler mRequestHandler;

    //A reference to the handler that is created in the main thread (which performs UI work); this
    //handler will be passed onto from the main thread to the ThumbnailDownloader thread; however,
    //it will always be associated to the main thread's looper
    private Handler mResponseHandler;

    //This interface allows us to set the downloaded Bitmap image to the ImageView that is in
    //the PhotoGalleryFragment. Thus, it allows us to separate the background thread from the UI
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    private LruCache<String, Bitmap> mCache;

    public interface ThumbnailDownloadListener<T>{
        void onThumbnailDownloaded(T target, Bitmap thumbnail);

//        Deprecated method: My own attempt at solving the preloading challenge
//        HashMap<T, String> obtainPreloadItems(T target);

    }

    //This hash map is used in order to store key-value pairs; in this case, the keys will be the
    //PhotoHolders and the values will be the URL strings
    private ConcurrentMap<T,String> mRequestMap = new ConcurrentHashMap<>();

    public ThumbnailDownloader(Handler responseHandler){
        super(TAG);
        mResponseHandler = responseHandler;
        mCache = new LruCache<>(16384);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener){
        mThumbnailDownloadListener = listener;
    }

    //This method is called before the message queue is checked for the first time; hence, this
    //makes it a good spot to create the Handler (message handler)
    @Override
    protected void onLooperPrepared(){
        mRequestHandler = new Handler(){
            //This method is used when a download message is pulled from the queue and is ready
            //to be processed
            @Override
            public void handleMessage(Message msg){
                switch (msg.what) {

                    case MESSAGE_DOWNLOAD:
                        T target = (T) msg.obj;
                        Log.i(TAG, "Got a request for a URL: " + mRequestMap.get(target));
                        handleRequest(target);
                        break;

                    case MESSAGE_PRELOAD:
                        String url = (String) msg.obj;
                        downloadImage(url);
                        break;
                }
            }
        };
    }

    private void handleRequest(final T target){
        final String url = mRequestMap.get(target);
        if(url == null) {
            Log.e(TAG, "URL not found for provided target");
            return;
        }

        final Bitmap bitmap = downloadImage(url);

        //post() allows us to have something performed without having to create
        //a message and sending it to the message queue; the handler does not actually
        //perform the action in post(); instead, the action is performed directly through post()
        mResponseHandler.post(new Runnable() {
            @Override
            public void run() {
                /**
                 * A RecyclerView is being used to display the images; this view "recycles" its
                 * views (PhotoHolders); by the time ThumbnailDownloader finishes downloading
                 * the image, the RecyclerView may have recycled the PhotoHolder and requested a
                 * new URL; the following check ensures that the correct image is loaded, even
                 * if another request is made in the mean time
                 **/
                if(mRequestMap.get(target) != url) {
                    Log.e(TAG, "URL not found for provided target");
                    return;
                }

                mRequestMap.remove(target);
                mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
//                    Depracated method from my attempt of solving the preloading challenge
//
//                    preloadCache(mThumbnailDownloadListener.obtainPreloadItems(target));
            }
        });
    }

    public void queueThumbnail(T target, String url){
        Log.i(TAG, "Got a URL: " + url);

        //If the URL is blank, don't include it in the hash map
        if(url == null)
            mRequestMap.remove(target);
        else{
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget();
        }
    }

    //Used to clear the message queue so that images are still correctly loaded upon screen
    //rotation
    public void clearQueue(){
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
    }

    public Bitmap getCachedImage(String url){
        return mCache.get(url);
    }

    public void preloadImage(String url){
        mRequestHandler.obtainMessage(MESSAGE_PRELOAD, url).sendToTarget();
    }

    /*
    * Deprecated method: My own attempt at solving the preloading challenge
    public void preloadCache(HashMap<T, String> photoHolders){
        Iterator iterator = photoHolders.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry pair = (Map.Entry) iterator.next();
            handleRequest((T) pair.getKey());
            Log.i(TAG, "Preloaded a URL: " + pair.getValue());
        }
    }
    */

    private Bitmap downloadImage(String url){
        //If somehow the URL was incorrectly used, return null
        if(url == null)
            return null;

        //If the image has already been cached, then extract it from the cache
        Bitmap bitmap = mCache.get(url);
        if(bitmap != null)
            return bitmap;

        //Otherwise, let's attempt to download the image and catch any exceptions that may occur
        //Place the newly-downloaded image into the cache
        try {
            byte[] bitmapBytes = new FlickrFetchr().getURLBytes(url);

            bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            mCache.put(url, bitmap);
            Log.i(TAG, "Downloaded and cached bitmap!");
            return bitmap;
        }

        catch(IOException ioe){
            Log.e(TAG, "Error downloading message", ioe);
            return null;
        }
    }

}
