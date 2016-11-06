package com.palarz.mike.photogallery;

import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

public class PhotoGalleryActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment(){
        return PhotoGalleryFragment.newInstance();
    }
}
