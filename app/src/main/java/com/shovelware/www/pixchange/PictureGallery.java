package com.shovelware.www.pixchange;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Picture;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

public class PictureGallery extends AppCompatActivity {
    String url;
    AsyncHttpClient httpClient;
    ArrayList<PictureData> pictures;
    ImageAdapter imgAdapter;
    String token;
    TabLayout tabs;

    ImageLoader imgLoader;
    private DisplayImageOptions options;
    Context mContext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mContext = this;
        final Context context = this;
        url = getString(R.string.server_url) + getString(R.string.upload_route);
        setContentView(R.layout.activity_picture_gallery);


        ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(this));

        imgLoader = ImageLoader.getInstance();
        options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.ic_dashboard_black_24dp)
                .showImageOnFail(R.drawable.loading_image)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .imageScaleType(ImageScaleType.EXACTLY)
                .displayer(new RoundedBitmapDisplayer(20))
                .build();


        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.picture_gallery_nav_bar);
        navigation.setSelectedItemId(R.id.navigation_gallery);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Intent intent;
                switch (item.getItemId()) {

                    case R.id.navigation_hunts:
                        intent = new Intent(context, HuntList.class);
                        startActivity(intent);
                        return true;
                    case R.id.navigation_home:
                        intent = new Intent(context, ParentActivity.class);
                        startActivity(intent);
                        return true;
                }
                return false;
            }
        });

        tabs = (TabLayout)findViewById(R.id.gallery_tabs);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0){
                    getGalleryData(true);
                } else{
                    getGalleryData(false);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        FloatingActionButton newPicture = (FloatingActionButton) findViewById(R.id.fab_takePictureIntent);
        newPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), take_image.class);
                startActivity(intent);
            }
        });
        final GridView galleryGrid = (GridView) findViewById(R.id.galleryView);
        pictures = new ArrayList<PictureData>();
        imgAdapter = new ImageAdapter(context, pictures);
        galleryGrid.setAdapter(imgAdapter);
        galleryGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(PictureGallery.this, "Clicked",Toast.LENGTH_SHORT).show();
            }
        });
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.pref_file_name), Context.MODE_PRIVATE);
        token = sharedPref.getString("token", "No Token");
        httpClient = new AsyncHttpClient();
        getGalleryData(true);


        galleryGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Toast.makeText(PictureGallery.this, "Clicked",Toast.LENGTH_SHORT).show();

                LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                View popupView = inflater.inflate(R.layout.picture_gallery_popup, null);
                //int width = LinearLayout.LayoutParams.MATCH_PARENT;
                //int height = LinearLayout.LayoutParams.WRAP_CONTENT;
                final int width = LinearLayout.LayoutParams.MATCH_PARENT;
                int height = LinearLayout.LayoutParams.MATCH_PARENT;
                // create the popup window


                boolean focusable = true; // lets taps outside the popup also dismiss it
                String imgURL = pictures.get(position).url;
                imgLoader.displayImage(imgURL, (ImageView) popupView.findViewById(R.id.gallery_popout_image), options, new ImageLoadingListener() {
                    @Override
                    public void onLoadingStarted(String imageUri, View view) {

                    }

                    @Override
                    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {

                    }

                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        Display display = getWindowManager().getDefaultDisplay();
                        DisplayMetrics outMetrics = new DisplayMetrics ();
                        display.getMetrics(outMetrics);

                        float density  = getResources().getDisplayMetrics().density;
                        float pxHeight = outMetrics.heightPixels;
                        float pxWidth  = outMetrics.widthPixels;

                        int width = loadedImage.getWidth();
                        int height = loadedImage.getHeight();
                        float scale = pxWidth / width;
                        int imageWidth = (int)pxWidth;
                        int imageHeight =(int)(height*scale);

                        if (imageHeight >= pxHeight*0.8){
                            double hScale = (pxHeight*0.8)/imageHeight;
                            imageHeight = (int) (pxHeight*0.8);
                            imageWidth = (int) (hScale * imageWidth);

                        }

                        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
                        params.width = imageWidth;
                        params.height = imageHeight;
                        view.setLayoutParams(params);

                    }

                    @Override
                    public void onLoadingCancelled(String imageUri, View view) {

                    }
                });
                final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

                // show the popup window
                // which view you pass in doesn't matter, it is only used for the window tolken
                popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

                // dismiss the popup window when touched
                popupView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        popupWindow.dismiss();
                        return true;
                    }
                });
            }
        });

    }



    public void getGalleryData(boolean showPersonalPictures){
        String route = "my_pictures?token="+token;
        if (!showPersonalPictures){
            route = "my_captured_hunts?token="+token;
        }
        httpClient.get(url + route, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                JSONObject res = null;
                boolean success = false;
                try {
                    res = new JSONObject(new String(responseBody));
                    success = res.getBoolean("success");
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(PictureGallery.this, "Error Parsing Response From Server. success value missing from response.",Toast.LENGTH_SHORT).show();
                }
                if (!success){
                    try {
                        int code = res.getInt("code");
                        if (code == 201){
                            Intent intent = new Intent(getBaseContext(), LoginActivity.class);
                            startActivity(intent);
                        }
                        String err = res.getString("err");
                        Toast.makeText(PictureGallery.this, err,Toast.LENGTH_SHORT).show();

                    } catch (JSONException e) {
                        Toast.makeText(PictureGallery.this, "Error Parsing Response From Server. err value missing from response.",Toast.LENGTH_SHORT).show();
                    }

                    return;
                }
                try {
                    JSONArray obj = res.getJSONArray("data");
                    Toast.makeText(PictureGallery.this, Integer.toString(obj.length()),Toast.LENGTH_SHORT).show();
                    pictures.clear();
                    for (int i=0;i<obj.length();i++) {
                        pictures.add(new PictureData(obj.getJSONObject(i)));
                    }
                    imgAdapter.notifyDataSetChanged();


                } catch (JSONException e) {

                    Toast.makeText(PictureGallery.this, "Error Parsing Response From Server. data value missing from response.",Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(PictureGallery.this, "Failed to access server.",Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume(){
        super.onResume();
        if (tabs.getSelectedTabPosition() == 0) {
            getGalleryData(true);
        } else{
            getGalleryData(false);
        }
    }
}
