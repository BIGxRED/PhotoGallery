package com.palarz.mike.photogallery;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mike on 11/2/16.
 */
public class PhotoGalleryFragment extends Fragment {
    private static final String TAG = "PhotoGalleryFragment";

    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
//    private boolean loading = true;
    private int mPageNumber;    //Keeps track of which page number to load in order to allow
                                // endless scrolling

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
            public void onScrolled(RecyclerView view, int dx, int dy){
                super.onScrolled(view, dx, dy);
                if(dy > 0){
                    //NO LONGER NEEDED: Only keeping this here since it was a creative workaround
                    //and may be needed for other situations
                    /**Obtain a reference to the layout manager and cast it as a LinearLayoutManager.
                    The casting is necessary in order to use findFirstVisibleItemPosition
                     **/
//                    LinearLayoutManager layoutManager = (LinearLayoutManager) mPhotoRecyclerView
//                            .getLayoutManager();

                    //If vertical scrolling is no longer possible (we've reached the end of the
                    //RecyclerView), fetch the next set of items
                    if(!mPhotoRecyclerView.canScrollVertically(1)){
                        new FetchItemsTask().execute(mPageNumber);
                    }
                }
            }
        });

        setupAdapter();

        return v;
    }

    private void setupAdapter(){
        if(isAdded()){
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
        private TextView mTitleTextView;

        public PhotoHolder(View itemView){
            super(itemView);
            mTitleTextView = (TextView) itemView;
        }

        public void bindGalleryItem(GalleryItem item){
            mTitleTextView.setText(item.toString());
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

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{
        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems){
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType){
            TextView textView = new TextView(getActivity());
            return new PhotoHolder(textView);
        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position){
            GalleryItem galleryItem = mGalleryItems.get(position);
            photoHolder.bindGalleryItem(galleryItem);
        }

        @Override
        public int getItemCount(){
            return mGalleryItems.size();
        }
    }

}
