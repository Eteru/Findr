package com.dsrv.ciprian.findr;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import static com.dsrv.ciprian.findr.AppConfig.TAG;


/**
 * Created by Eteru on 11/11/2017.
 */

public class PlacesController extends Application {

    private RequestQueue mRequestQueue;
    private static Context mContext;
    private static PlacesController mInstance = null;

    protected PlacesController() {
        // Exists only to defeat instantiation.
    }

    public static synchronized PlacesController getInstance() {
        if(mInstance == null) {
            mInstance = new PlacesController();
        }
        return mInstance;
    }

    public static void SetContext(Context context) {
        mContext = context;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mContext);
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req, String tag) {
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
        getRequestQueue().add(req);
    }

    public <T> void addToRequestQueue(Request<T> req) {
        req.setTag(TAG);
        getRequestQueue().add(req);
    }

    public void cancelPendingRequests(Object tag) {
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(tag);
        }
    }
}
