package com.shovelware.www.pixchange;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.Header;
import okhttp3.ConnectionSpec;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class take_image extends AppCompatActivity implements OnMapReadyCallback, LocationListener {
    boolean canAccessLocation = false;
    boolean uploadReady = false;
    ImageView image;
    Uri imageURI;
    File imageFile;
    static final int NEW_CONVO_IMAGE = 0;
    GoogleMap googleMap;
    MapView map;
    Marker posMarker = null;
    protected LocationManager locManager;
    protected LocationListener locListener;
    final int REQUEST_LOCATION_CODE = 301;
    protected Location loc = null;
    String url;
    AsyncHttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_image);
        File[] files = getFilesDir().listFiles();
        if (files != null){
            for (File f: files){
                f.delete();
            }
        }
        url = getString(R.string.server_url)+getString(R.string.upload_route);
        httpClient = new AsyncHttpClient();


        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        FloatingActionButton take_picture_fab = (FloatingActionButton) findViewById(R.id.take_picture_fab);
        take_picture_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
                takePicture();

            }
        });
        FloatingActionButton upload_picture_fab = (FloatingActionButton) findViewById(R.id.upload_picture_fab);
        upload_picture_fab.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                uploadPicture();
            }
        });
        image = (ImageView) findViewById(R.id.imageView);
        GoogleMapOptions options = new GoogleMapOptions();
        options.liteMode(true);

        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                TextView t = (TextView) findViewById(R.id.loc_text);
                if (googleMap == null) return;
                if (loc == null){
                    //First Time Pos Found, add marker
                    loc = location;
                    LatLng myPos = new LatLng(loc.getLatitude(), loc.getLongitude());
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPos, 16.0f));
                    posMarker = googleMap.addMarker(new MarkerOptions().position(myPos));
                    t.setText(loc.getLatitude()+"   "+loc.getLongitude());
                    return;
                }
                loc = location;
                LatLng myPos = new LatLng(loc.getLatitude(), loc.getLongitude());
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPos, 16.0f));
                posMarker.setPosition(myPos);
                t.setText(loc.getLatitude()+"   "+loc.getLongitude());

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
            canAccessLocation = true;
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locListener);
        }

        map = (MapView) findViewById(R.id.location_map);
        map.onCreate(savedInstanceState);
        map.getMapAsync(this);
        MapsInitializer.initialize(this);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    canAccessLocation = true;
                    try {
                        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locListener);
                    } catch (SecurityException e){
                        Intent intent = new Intent(getBaseContext(), ParentActivity.class);
                        startActivity(intent);
                    }
                } else {
                    Intent intent = new Intent(getBaseContext(), ParentActivity.class);
                    startActivity(intent);
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_convo_select, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        System.out.println("Result Code: "+resultCode);
        if (requestCode == NEW_CONVO_IMAGE && resultCode == RESULT_OK){
            //BitmapFactory.Options options = new BitmapFactory.Options();
            //options.inPreferredConfig = Bitmap.Config.ARGB_8888;
           //Bitmap bitmap = BitmapFactory.decodeFile(imageURI, options);
            image.setImageURI(imageURI);
            //Bundle extras = data.getExtras();
            //Bitmap imageBitmap = (Bitmap) extras.get("data");
            //image.setImageBitmap(imageBitmap);
            uploadReady = true;
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getFilesDir();
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        //mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }
    public void uploadPicture(){
        if (loc == null){
            createDialog("Error","No Location found.");
            return;
        }
        if (!uploadReady) {
            createDialog("Error","No Picture Taken.");
            return;
        }
        RequestParams params = new RequestParams();
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.pref_file_name), Context.MODE_PRIVATE);
        String token = sharedPref.getString("token", "No Token");
        params.add("token", token);
        params.add("lat", Double.toString(loc.getLatitude()));
        params.add("lng", Double.toString(loc.getLongitude()));
        params.add("file", imageURI.toString());
        Toast.makeText(take_image.this, "Upload Started (really need a loading bar here)", Toast.LENGTH_SHORT).show();
        httpClient.post(url + "new_picture", params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    JSONObject res = new JSONObject(new String(responseBody));
                    boolean success;
                    success = res.getBoolean("success");
                    if (!success){
                        int code = res.getInt("code");
                        if (code == 201){
                            Intent intent = new Intent(getBaseContext(), LoginActivity.class);
                            startActivity(intent);
                        }
                        String err = res.getString("err");
                        createDialog("Error", err);
                        return;
                    }
                    final String signed_url = res.getString("sign");
                    new Thread(new Runnable(){
                        public void run(){
                            OkHttpClient client = new OkHttpClient.Builder()
                                    .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT))
                                    .connectTimeout(30, TimeUnit.SECONDS)
                                    .readTimeout(30, TimeUnit.SECONDS)
                                    .writeTimeout(30, TimeUnit.SECONDS)
                                    .build();
                            Request uploadFileRequest = new Request.Builder()
                                    .url(signed_url)
                                    .put(RequestBody.create(MediaType.parse("image/jpg"), imageFile))
                                    .build();
                            try {
                                Response uploadResponse = client.newCall(uploadFileRequest).execute();
                                if (uploadResponse.isSuccessful()){
                                    image.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            image.setImageDrawable(null);
                                        }
                                    });
                                    imageFile.delete();
                                    imageFile = null;
                                    uploadReady = false;
                                    take_image.this.runOnUiThread(new Runnable() {
                                        public void run() {
                                            Toast.makeText(take_image.this, "Upload Success", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                /*
                                if (uploadResponse.isSuccessful()){
                                    take_image.this.runOnUiThread(new Runnable() {
                                        public void run() {
                                            Toast.makeText(take_image.this, "Upload Success", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    image.setImageDrawable(null);
                                    imageFile = null;
                                    uploadReady = false;
                                } else{
                                    //Toast.makeText(take_image.this, "Failed to access server.",Toast.LENGTH_SHORT).show();
                                }
                                */

                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }).start();
                } catch (JSONException e) {
                    createDialog("Error", "JSON error");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                createDialog("Error", "Post error");
            }
        });
    }
    public void takePicture(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("EST"));

            imageFile = null;
            try {
                imageFile = createImageFile();
            } catch (IOException e){
            }
            //File photo = new File(imagePath, "default_image.jpg");
            //File photo = new File(Environment.getExternalStorageDirectory(), timeStamp);


            //imageURI = Uri.fromFile(imagePath);
            imageURI = FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() +  ".provider", imageFile);

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageURI);

            startActivityForResult(takePictureIntent, NEW_CONVO_IMAGE);


        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.getUiSettings().setCompassEnabled(false);
        this.googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        this.googleMap.getUiSettings().setMapToolbarEnabled(false);
    }

    @Override
    public void onLocationChanged(Location location) {
        TextView t = (TextView) findViewById(R.id.loc_text);

        if (loc == null){
            createDialog("Map","Found Loc!");
            //First Time Pos Found, add marker
            loc = location;
            LatLng myPos = new LatLng(loc.getLatitude(), loc.getLongitude());
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPos, 16.0f));
            //posMarker = googleMap.addMarker(new MarkerOptions().position(myPos));
            t.setText(loc.getLatitude()+"   "+loc.getLongitude());
            return;
        }
        loc = location;
        LatLng myPos = new LatLng(loc.getLatitude(), loc.getLongitude());
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPos, 16.0f));
        t.setText(loc.getLatitude()+"   "+loc.getLongitude());

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

    protected void createDialog(String title, String content){
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(content);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }
}
