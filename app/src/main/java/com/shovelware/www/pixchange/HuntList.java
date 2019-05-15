package com.shovelware.www.pixchange;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
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
import org.w3c.dom.Text;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

public class HuntList extends AppCompatActivity {
    protected LocationManager locManager;
    protected LocationListener locListener;
    final int REQUEST_LOCATION_CODE = 301;
    protected Location loc = null;
    protected boolean completedFirstLocationUpdate = false;
    ArrayList<HuntData> hunts;
    HuntAdapter huntAdapter;
    String url;
    AsyncHttpClient httpClient;

    ImageLoader imgLoader;
    private DisplayImageOptions options;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = this;
        setContentView(R.layout.activity_hunt_list);
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
        navigation.setSelectedItemId(R.id.navigation_hunts);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Intent intent;
                switch (item.getItemId()) {

                    case R.id.navigation_gallery:
                        intent = new Intent(context, PictureGallery.class);
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
        hunts = new ArrayList<HuntData>();
        huntAdapter = new HuntAdapter(context, hunts);
        final GridView galleryGrid = (GridView) findViewById(R.id.huntListView);
        url = getString(R.string.server_url)+getString(R.string.upload_route);
        httpClient = new AsyncHttpClient();
        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                loc = location;
                if (!completedFirstLocationUpdate) {
                    completedFirstLocationUpdate = true;
                    RequestParams params = new RequestParams();
                    SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.pref_file_name), Context.MODE_PRIVATE);
                    String token = sharedPref.getString("token", "No Token");
                    params.add("token", token);
                    params.add("lat", Double.toString(loc.getLatitude()));
                    params.add("lng", Double.toString(loc.getLongitude()));
                    httpClient.post(url + "find_nearby_hunts", params, new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            JSONObject res = null;
                            try {
                                res = new JSONObject(new String(responseBody));
                                boolean success;
                                success = res.getBoolean("success");
                                if (!success) {
                                    int code = res.getInt("code");
                                    if (code == 201) {
                                        Intent intent = new Intent(getBaseContext(), LoginActivity.class);
                                        startActivity(intent);
                                    }
                                    String err = res.getString("err");
                                    Toast.makeText(HuntList.this, err,Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                //Handle Correct Data response from server

                                JSONArray obj = res.getJSONArray("data");
                                Toast.makeText(HuntList.this, Integer.toString(obj.length()),Toast.LENGTH_SHORT).show();
                                for (int i=0;i<obj.length();i++) {
                                    hunts.add(new HuntData(obj.getJSONObject(i)));
                                    hunts.get(i).getCurrentDistance(loc.getLatitude(), loc.getLongitude());
                                }

                                galleryGrid.setAdapter(huntAdapter);
                                galleryGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                        createPopupImage(hunts.get(position));
                                    }
                                });
                            }
                             catch (JSONException e) {
                                 Toast.makeText(HuntList.this, "Error handling server response.",Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                            Toast.makeText(HuntList.this, "Failed to connect to server.",Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                for (int i=0;i<hunts.size();i++){
                    hunts.get(i).getCurrentDistance(loc.getLatitude(), loc.getLongitude());
                }
               huntAdapter.notifyDataSetChanged();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_CODE);
        } else{
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locListener);
        }
    }

    public void createPopupImage(HuntData hunt){
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.hunt_list_popup, null);
        //int width = LinearLayout.LayoutParams.MATCH_PARENT;
        //int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;
        // create the popup window


        boolean focusable = true; // lets taps outside the popup also dismiss it
        String imgURL = hunt.url;
        imgLoader.displayImage(imgURL, (ImageView) popupView.findViewById(R.id.hunt_popout_image), options, new ImageLoadingListener() {
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
        popupWindow.showAtLocation(getCurrentFocus(), Gravity.CENTER, 0, 0);

        // dismiss the popup window when touched
        popupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                popupWindow.dismiss();
                return true;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    try {
                        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locListener);
                    } catch (SecurityException e){
                        noLocationAccessAlert();
                    }
                } else {
                    noLocationAccessAlert();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }
    public void noLocationAccessAlert(){
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("GPS Location Required");
        alertDialog.setMessage("Location Access is required for functionality.");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

}
