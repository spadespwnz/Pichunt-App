package com.shovelware.www.pixchange;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nathan on 1/28/2019.
 */

public class ImageAdapter extends BaseAdapter {
    private Context mContext;
    ArrayList<PictureData> imageURLs;
    ImageLoader imgLoader;
    private DisplayImageOptions options;
    public ImageAdapter(Context c, ArrayList urls){
        imageURLs = urls;
        mContext = c;
        imgLoader = ImageLoader.getInstance();
        options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.ic_dashboard_black_24dp)
                .showImageOnFail(R.drawable.loading_image)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .displayer(new RoundedBitmapDisplayer(20))
                .build();
    }
    @Override
    public int getCount() {
        return imageURLs.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null){
            convertView = new ImageView(mContext);
            ((ImageView) convertView).setAdjustViewBounds(true);
        }
;
        String url = imageURLs.get(position).url+"-thumbnail";

        imgLoader.displayImage(url, (ImageView) convertView, options, new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {

            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {

            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {

            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {

            }
        });

        //new PictureLoadTask(url, (ImageView)convertView).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);

        return convertView;
    }
}

class PictureLoadTask extends AsyncTask<Void,Void,Bitmap>{
    String url;
    ImageView iv;

    public PictureLoadTask(String url, ImageView image){
        this.url = url;
        iv = image;
    }
    @Override
    protected Bitmap doInBackground(Void... params) {
        try {
            System.out.println(url);
            URL myFileUrl = new URL (url);
            HttpURLConnection conn =
                    (HttpURLConnection) myFileUrl.openConnection();
            conn.setDoInput(true);
            conn.connect();

            InputStream is = conn.getInputStream();
            return(BitmapFactory.decodeStream(is));


        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    protected void onPostExecute(Bitmap bitmap){
        iv.setImageBitmap(bitmap);
    }
}