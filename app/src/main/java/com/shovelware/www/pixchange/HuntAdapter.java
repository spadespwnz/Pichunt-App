package com.shovelware.www.pixchange;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.Image;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

/**
 * Created by Nathan on 2/1/2019.
 */

public class HuntAdapter extends BaseAdapter {
    private Context mContext;
    ArrayList<HuntData> hunts;
    ImageLoader imgLoader;
    private DisplayImageOptions options;

    String url;
    AsyncHttpClient httpClient;

    public HuntAdapter(Context c, ArrayList<HuntData> hunts){
        mContext = c;
        url = c.getString(R.string.server_url)+c.getString(R.string.upload_route);
        httpClient = new AsyncHttpClient();
        this.hunts = hunts;
        imgLoader = ImageLoader.getInstance();
        options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.ic_dashboard_black_24dp)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .displayer(new RoundedBitmapDisplayer(20))
                .build();
    }
    @Override
    public int getCount() {
        return hunts.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }


    public void collectHunt(HuntData hunt){
        if (hunt.distance > Constant.CAPTURE_DISTANCE){
            Toast.makeText(mContext, "Too Far Away", Toast.LENGTH_SHORT).show();
            return;
        }
        RequestParams params = new RequestParams();
        SharedPreferences sharedPref = mContext.getApplicationContext().getSharedPreferences(
                mContext.getString(R.string.pref_file_name), Context.MODE_PRIVATE);
        String token = sharedPref.getString("token", "No Token");
        params.add("token", token);
        params.add("distance", Double.toString(hunt.distance));
        params.add("_id", hunt.id);
        httpClient.post(url + "capture_hunt", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    JSONObject res = new JSONObject(new String(responseBody));
                    boolean success = res.getBoolean("success");
                    if (success == false){
                        String err = res.getString("err");
                        Toast.makeText(mContext, err, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String delId = res.getString("id");
                    HuntData delHunt = null;


                    for (HuntData hunt : hunts){
                        if (hunt.id.equals(delId)){
                            delHunt = hunt;
                            break;
                        }
                    }
                    if (delHunt != null){
                        hunts.remove(delHunt);
                        notifyDataSetChanged();
                        Toast.makeText(mContext, "Hunt Captured!", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    Toast.makeText(mContext, "Error Parsing Server Response.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        if (convertView == null){
            LayoutInflater inf = LayoutInflater.from(mContext);
            convertView = inf.inflate(R.layout.hunt_row, parent,false);
            vh = new ViewHolder();
            vh.url = hunts.get(position).url+"-thumbnail";
            vh.image = (ImageView) convertView.findViewById(R.id.hunt_image_view);

            vh.collectButton = (Button) convertView.findViewById(R.id.hunt_row_collect_button);
            vh.collectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    collectHunt(hunts.get(position));
                }
            });

            vh.userText = (TextView) convertView.findViewById(R.id.hunt_owner);
            vh.distanceText = (TextView) convertView.findViewById(R.id.hunt_distance);
            convertView.setTag(vh);
            vh.userText.setText(hunts.get(position).owner);
            vh.distanceText.setText(Double.toString(hunts.get(position).distance));
            imgLoader.displayImage(vh.url, vh.image, options);

            setBGColor(convertView, hunts.get(position).distance);
            updateCollectButton(vh.collectButton, hunts.get(position).distance);
            //new PictureLoadTask(vh.url, vh.image).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
            return convertView;


        } else{
            vh = (ViewHolder) convertView.getTag();
        }

        //If its the same data just reuse
        if (vh.url.equals(hunts.get(position).url)){
            setBGColor(convertView, hunts.get(position).distance);
            updateCollectButton(vh.collectButton, hunts.get(position).distance);
            vh.distanceText.setText(Double.toString(hunts.get(position).distance));
            return convertView;
         //Recreate view if needed
        } else{
            setBGColor(convertView, hunts.get(position).distance);
            updateCollectButton(vh.collectButton, hunts.get(position).distance);
            vh.userText.setText(hunts.get(position).owner);
            vh.distanceText.setText(Double.toString(hunts.get(position).distance));
            vh.url = hunts.get(position).url+"-thumbnail";
            vh.image.setImageDrawable(null);
            imgLoader.displayImage(vh.url, vh.image, options);
            return convertView;
        }

    }
    public void updateCollectButton(Button b, Double dis){
        if (dis > Constant.CAPTURE_DISTANCE){
            if (b.getVisibility() != View.GONE) b.setVisibility(View.GONE);
        } else{
            if (b.getVisibility() != View.VISIBLE) b.setVisibility(View.VISIBLE);
        }
    }
    public void setBGColor(View v, Double dis){
        int color;
        if (dis < 5){
            color = 0xFF00FF00;
        } else if (dis < 50){
            color = 0xFFB0FF00;
        } else if (dis < 200){
            color = 0xFFFFFF00;
        } else if (dis < 800){
            color = 0xFFFFB000;
        } else if (dis < 2000){
            color = 0xFFFF0000;
        } else{
            color = 0xFFFF00FF;
        }
        //Correct color already set
        if (v.getBackground() != null) {
            if (((ColorDrawable) v.getBackground()).getColor() == color) {
                return;
            }
        }

        ((RelativeLayout)v).setBackgroundColor(color);

    }
}

class ViewHolder{
    ImageView image;
    TextView userText;
    TextView distanceText;
    Button collectButton;
    String url = "";
}