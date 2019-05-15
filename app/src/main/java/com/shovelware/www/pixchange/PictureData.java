package com.shovelware.www.pixchange;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Nathan on 1/28/2019.
 */

public class PictureData {
    String owner;
    double lat;
    double lng;
    String url;
    String id;
    public PictureData(JSONObject pic){
        try {
            owner = pic.getString("owner");
            lat = pic.getDouble("lat");
            lng = pic.getDouble("lng");
            url = pic.getString("url");
            id = pic.getString("_id");
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
