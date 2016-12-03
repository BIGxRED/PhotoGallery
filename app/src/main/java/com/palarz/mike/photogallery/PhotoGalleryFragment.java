package com.palarz.mike.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mike on 11/2/16.
 */
public class PhotoGalleryFragment extends Fragment {
    private static final String TAG = "PhotoGalleryFragment";

    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();

    //Keeps track of which page number to load in order to allow endless scrolling
    private int mPageNumber;

    //An instance of the ThumbnailDownloader class; PhotoHolder is being used as the Type argument
    //because this argument specifies the type of object that will be used as the identifier of the
    //download
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //Used so that the same async thread is used upon rotation and other system changes
        //More specifically, it ensures that the same fragment instance is retained across
        //Activity recreation
        setRetainInstance(true);
        mPageNumber = 1;
        new FetchItemsTask().execute(mPageNumber);

        //This handler will be attached to the main UI thread and will be associated to that
        //thread's Looper; it is only passed on to the ThumbnailDownloader thread in order to
        //retrieve the Bitmap needed for the Flickr images
        //The reason that this handler is associated to the main thread is because it has been
        //within onCreate()
        Handler responseHandler = new Handler();

        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(
            new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
                @Override
                public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap) {
                    Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                    photoHolder.bindDrawable(drawable);

                }

                /*
                * Deprecated method: My own attempt at solving the preloading challenge
                @Override
                public HashMap<PhotoHolder, String> obtainPreloadItems(PhotoHolder photoHolder){
                    //Preload cache challenge
                    int currentPosition = photoHolder.getAdapterPosition();
                    LinearLayoutManager layoutManager = (LinearLayoutManager) mPhotoRecyclerView
                            .getLayoutManager();
                    int lowestItem = currentPosition - 10;
                    if(lowestItem < layoutManager.findFirstVisibleItemPosition()){
                        while(lowestItem < layoutManager.findFirstVisibleItemPosition()){
                            lowestItem++;
                        }
                    }

                    int highestItem = currentPosition + 10;
                    if(highestItem > layoutManager.findLastVisibleItemPosition()){
                        while(highestItem > layoutManager.findLastVisibleItemPosition()){
                            highestItem--;
                        }
                    }
                    HashMap<PhotoHolder, String> itemsToPreload = new HashMap<PhotoHolder, String>(20);
                    for(int i = lowestItem; i < highestItem + 1; i++){
                        PhotoHolder currentHolder = (PhotoHolder) mPhotoRecyclerView
                                .findViewHolderForLayoutPosition(i);
                        itemsToPreload.put(currentHolder, mItems.get(i).getURL());
                    }
                    return itemsToPreload;
                }
                */

            }
        );
        mThumbnailDownloader.start();

        //getLooper() is called in order to ensure that the threads "guts" are ready before
        //proceeding; also removes the possibility of a potential race condition occurring; this is
        //because until getLooper() is called, there is no guarantee that onLooperPrepared() has
        //also been called
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread has started");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mPhotoRecyclerView = (RecyclerView) v
                .findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView view, int dx, int dy) {
                super.onScrolled(view, dx, dy);
                if (dy > 0) {
                    //NO LONGER NEEDED: Only keeping this here since it was a creative workaround
                    //and may be needed for other situations
                    /**Obtain a reference to the layout manager and cast it as a LinearLayoutManager.
                     The casting is necessary in order to use findFirstVisibleItemPosition
                     **/
//                    LinearLayoutManager layoutManager = (LinearLayoutManager) mPhotoRecyclerView
//                            .getLayoutManager();

                    //If vertical scrolling is no longer possible (we've reached the end of the
                    //RecyclerView, which is what the 1 specifies), fetch the next set of items
                    if (!mPhotoRecyclerView.canScrollVertically(1)) {
                        new FetchItemsTask().execute(mPageNumber);
                    }
                }
            }
        });

        setupAdapter();

        return v;
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();  //Clear the message queue so that the images are loaded
                                            //correctly upon screen rotation
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        //Thread is destroyed in order to ensure that it is terminated; otherwise, it will never die
        //and continue to live after the app is closed/destroyed
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }

    private void setupAdapter(){
        if(isAdded()){
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
//        private TextView mTitleTextView;
        private ImageView mItemImageView;

        public PhotoHolder(View itemView){
            super(itemView);
//            mTitleTextView = (TextView) itemView;
            mItemImageView = (ImageView) itemView
                    .findViewById(R.id.fragment_photo_gallery_image_view);
        }

//        public void bindGalleryItem(GalleryItem item){
//            mTitleTextView.setText(item.toString());
//        }
        public void bindDrawable(Drawable drawable){
            mItemImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{
        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems){
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType){
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, viewGroup, false);
            return new PhotoHolder(view);
        }

        //This method has been adjusted in order to implement preloading
        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position){
            GalleryItem galleryItem = mGalleryItems.get(position);

            //First, let's see if the image has already been cached
            Bitmap bitmap = mThumbnailDownloader.getCachedImage(galleryItem.getURL());

            //If the image has not been cached, then we will need to download it accordingly
            if(bitmap == null) {
                Drawable placeHolder = getResources().getDrawable(R.drawable.bill_up_close);
                photoHolder.bindDrawable(placeHolder);
                mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getURL());
            }

            //Otherwise, we can load in the cached image
            else{
                Log.i(TAG, "Image from cache has been loaded");
                photoHolder.bindDrawable(new BitmapDrawable(getResources(), bitmap));
            }
            obtainAdjacentImages(position);
        }

        @Override
        public int getItemCount(){
            return mGalleryItems.size();
        }

        private void obtainAdjacentImages(int position){
            final int preloadBufferSize = 10;
            int startIndex = Math.max(position - preloadBufferSize, 0);
            int endIndex = Math.min(position + preloadBufferSize, mGalleryItems.size() - 1);

            for(int i = startIndex; i < endIndex; i++){
                //If i is on the image that the adapter is currently tied to, skip it and move
                //onto the next image
                if(i == position)
                    continue;
                mThumbnailDownloader.preloadImage(mGalleryItems.get(i).getURL());
            }
        }
    }

    /**
     * The three input parameters to ASyncTask are as follows:
     *
     * 1st parameter: The set of parameters that are used by the execute() method, which dictates
     * the type of parameters doInBackground() will receive
     *
     * 2nd parameter: Used to send progress updates of the background thread; paramter is passed
     * into publishProgress()
     *
     * 3rd parameter: The result produced by the ASyncTask; it sets the return type of
     * doInBackground() and the input parameter of onPostExecute()
     */
    private class FetchItemsTask extends AsyncTask<Integer, Void, List<GalleryItem>>{
        @Override
        protected List<GalleryItem> doInBackground(Integer... params){
            return new FlickrFetchr().fetchItems(mPageNumber);
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items){
            //If mItems has already been set and more pages have been fetched, append those pages
            //to mItems
            if (mPageNumber > 1){
                //Add the new GalleryItems to mItems
                mItems.addAll(items);
                //Force the adapter to reload all ViewHolders; this is necessary, otherwise new
                //GalleryItems will not be loaded
                mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
            }
            //If the first page is fetched (mPageNumber == 1), initialized mItems accordingly
            else {
                mItems = items;
                setupAdapter();
            }
            mPageNumber++;
        }
    }

}
