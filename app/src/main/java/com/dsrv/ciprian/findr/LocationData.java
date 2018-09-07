package com.dsrv.ciprian.findr;

import android.graphics.Bitmap;
import android.location.Location;

/**
 * Created by Eteru on 11/29/2017.
 */

public class LocationData {
    String mId;
    String mPlaceId;
    String mPlaceName;
    String mReference;
    String mIcon;
    String mVicinity;
    boolean mOpen;
    double mRating;

    float mX, mY;
    float mDistance;
    Location mLocation;
    Bitmap mIconBitmap = null;

    public LocationData(String id, String place_id, String place_name, String reference,
                        String icon, String vicinity, boolean open, double rating,
                        double latitude, double longitude) {

        mId = id;
        mPlaceId = place_id;
        mPlaceName = place_name;
        mReference = reference;
        mIcon = icon;
        mVicinity = vicinity;
        mOpen = open;
        mRating = rating;

        mX = 0;
        mY = 0;

        mLocation = new Location("manual");
        mLocation.setLatitude(latitude);
        mLocation.setLongitude(longitude);
        mLocation.setAltitude(0);
    }

    public void setDistance(float distance) {
        mDistance = distance;
    }

    public void setCoords(float x, float y) {
        mX = x;
        mY = y;
    }

    public boolean isInside(float coords[]) {
        if (null == mIconBitmap) {
            return false;
        }

        float dx = Math.abs(coords[0] - mX);
        float dy = Math.abs(coords[1] - mY);
        float R = mIconBitmap.getHeight() * 2;

        if (dx > R || dy > R) {
            return false;
        }

        return true;
    }

}
