package com.shovelware.www.pixchange;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Nathan on 1/28/2019.
 */

public class HuntData {
    String owner;
    double lat;
    double lng;
    double distance;
    String url;
    String id;
    public HuntData(JSONObject pic){
        try {
            owner = pic.getString("owner");
            lat = pic.getDouble("lat");
            lng = pic.getDouble("lng");
            url = pic.getString("url");
            id = pic.getString("_id");
            //distance = pic.getDouble("distance");
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
    public void getCurrentDistance(double myLat,double myLon) {
        double R = 6371; // Radius of the earth in km
        double dLat = deg2rad(myLat-lat);  // deg2rad below
        double dLon = deg2rad(myLon-lng);
        double a =
                Math.sin(dLat/2) * Math.sin(dLat/2) +
                        Math.cos(deg2rad(myLat)) * Math.cos(deg2rad(lat)) *
                                Math.sin(dLon/2) * Math.sin(dLon/2)
                ;
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = R * c; // Distance in km
        distance = Math.round(d*1000);

    }
    public double deg2rad(double deg) {
        return deg * (Math.PI/180);
    }
}
