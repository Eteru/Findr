package com.dsrv.ciprian.findr;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.dsrv.ciprian.findr.AppConfig.GEOMETRY;
import static com.dsrv.ciprian.findr.AppConfig.GOOGLE_BROWSER_API_KEY;
import static com.dsrv.ciprian.findr.AppConfig.ICON;
import static com.dsrv.ciprian.findr.AppConfig.IS_OPEN;
import static com.dsrv.ciprian.findr.AppConfig.LATITUDE;
import static com.dsrv.ciprian.findr.AppConfig.LOCATION;
import static com.dsrv.ciprian.findr.AppConfig.LONGITUDE;
import static com.dsrv.ciprian.findr.AppConfig.NAME;
import static com.dsrv.ciprian.findr.AppConfig.OK;
import static com.dsrv.ciprian.findr.AppConfig.OPENING_HOURS;
import static com.dsrv.ciprian.findr.AppConfig.PLACE_ID;
import static com.dsrv.ciprian.findr.AppConfig.RATING;
import static com.dsrv.ciprian.findr.AppConfig.REFERENCE;
import static com.dsrv.ciprian.findr.AppConfig.STATUS;
import static com.dsrv.ciprian.findr.AppConfig.SUPERMARKET_ID;
import static com.dsrv.ciprian.findr.AppConfig.VICINITY;
import static com.dsrv.ciprian.findr.AppConfig.ZERO_RESULTS;

/**
 * Created by Eteru on 10/15/2017.
 */

public class OverlayView extends View implements SensorEventListener, LocationListener {

    public static final String DEBUG_TAG = "OverlayView Log";

    private Context mContext = null;
    static public Location mCrtLocation = null;
    private LocationManager mLocationManager = null;

    private ComputeLocations mComputeLocations = null;

    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);;
    private Paint mPaintLocation = new Paint(Paint.ANTI_ALIAS_FLAG);;

    private float mRotateFactor;
    private float mLastCompassData[] = new float[3];
    private float mLastAccelerometerData[] = new float[3];

    private boolean isAccelAvailable = false;
    private boolean isCompassAvailable = false;
    private boolean isGyroAvailable = false;
    private boolean mLocationTaskStarted = false;

    private Pair<Float, Float> mCenterPos = null;

    private boolean mDontDraw = false;

    ArDisplayView cameraData;

    List<LocationData> mLocations = new ArrayList<>();

    // Run a task every 10 seconds
    TimerTask mLocationTask = new LocationsTask();
    Timer mTimer = null;

    // Invallidate every at N fps

    final int TICKS_PER_SECOND = 60;
    final int SKIP_TICKS = 1000 / TICKS_PER_SECOND;
    TimerTask mInvallidateTask = new LocationsTask();
    Timer mTimerInvallidate = null;


    private static final Map<String, String> mLocationTypeById;
    static
    {
        mLocationTypeById = new HashMap<>();
        mLocationTypeById.put("0", "atm");
        mLocationTypeById.put("1", "bus_station");
        mLocationTypeById.put("2", "cafe");
        mLocationTypeById.put("3", "restaurant");
        mLocationTypeById.put("4", "meal_takeaway");
        mLocationTypeById.put("5", "bar");
        mLocationTypeById.put("6", "night_club");
    }

    public OverlayView(Context context, ArDisplayView arDisplayView) {
        super(context);

        mContext = context;
        this.setBackgroundColor(Color.TRANSPARENT);
        cameraData = arDisplayView;

        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor accelSensor = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor compassSensor = sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor gyroSensor = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        isAccelAvailable = sensors.registerListener(this, accelSensor, 500000, 100000);
        isCompassAvailable = sensors.registerListener(this, compassSensor, 500000, 100000);
        isGyroAvailable = sensors.registerListener(this, gyroSensor, 500000, 100000);

        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);

        String best = mLocationManager.getBestProvider(criteria, true);

        Log.v(DEBUG_TAG,"Best provider: " + best);

        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            mLocationManager.requestLocationUpdates(best, 1000, 1, this);
            mCrtLocation = mLocationManager.getLastKnownLocation(best);

            if (false == mLocationTaskStarted) {
                // Run the location grabbing task every 10 seconds
                mTimer = new Timer(true);
                mTimer.scheduleAtFixedRate(mLocationTask, 0, 3 * 1000);
                mLocationTaskStarted = true;
            }
        }

        mInvallidateTask = new DrawTask();
        mTimerInvallidate = new Timer(true);
        mTimerInvallidate.scheduleAtFixedRate(mInvallidateTask, 0, SKIP_TICKS);


        // location paint
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setTextSize(20);
        mPaint.setColor(Color.WHITE);
        mPaintLocation.setStrokeWidth(5);
        mPaintLocation.setStyle(Paint.Style.STROKE);
        mPaintLocation.setColor(Color.BLACK);
        mPaintLocation.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (true == mDontDraw) {
            return;
        }

        super.onDraw(canvas);

        if (null == mCenterPos) {
            mCenterPos = new Pair<>((float)canvas.getWidth()/2, (float)canvas.getHeight()/2);
        }

        // use roll for screen rotation
        canvas.rotate(mRotateFactor);

        // Debug line for rotations
        canvas.drawLine(-canvas.getHeight(), mCenterPos.second, canvas.getWidth()+canvas.getHeight(), mCenterPos.second, mPaint);

        // Draw locations
        for (LocationData ld : mLocations) {
            float size = 15.f;

            if (null != ld.mIconBitmap) {
                size = ld.mIconBitmap.getHeight();
                canvas.drawCircle(ld.mX, ld.mY, size, mPaintLocation);
                canvas.drawCircle(ld.mX, ld.mY, size, mPaint);
                canvas.drawBitmap(ld.mIconBitmap, ld.mX - ld.mIconBitmap.getWidth() / 2,
                        ld.mY - ld.mIconBitmap.getHeight() / 2, mPaint);

                canvas.drawText(ld.mPlaceName, ld.mX - ld.mIconBitmap.getWidth() / 2, ld.mY + size + 20.f, mPaint);
            }
            else {
                canvas.drawCircle(ld.mX, ld.mY, size, mPaintLocation);
                canvas.drawCircle(ld.mX, ld.mY, size, mPaint);
                canvas.drawText(ld.mPlaceName, ld.mX, ld.mY + size + 20.f, mPaint);
            }

        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        StringBuilder msg = new StringBuilder(event.sensor.getName()).append(" ");

        for(float value: event.values)  {
            msg.append("[").append(value).append("]");
        }

        switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                mLastAccelerometerData = lowPassFilter(event.values, mLastAccelerometerData);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mLastCompassData = lowPassFilter(event.values, mLastCompassData);
                break;
        }

        if (null == mComputeLocations) {
            mComputeLocations = new ComputeLocations(getWidth(), getHeight(), mLastAccelerometerData, mLastCompassData, mCrtLocation);
            mComputeLocations.execute();
        }
        else {
            if (AsyncTask.Status.FINISHED == mComputeLocations.getStatus()) {
                mComputeLocations = new ComputeLocations(getWidth(), getHeight(), mLastAccelerometerData, mLastCompassData, mCrtLocation);
                mComputeLocations.execute();
            }
        }
    }

    private float[] lowPassFilter(float[] values, float[] prevValues) {
        if ( prevValues == null ) return values;

        float ALPHA = 0.4f;
        for (int i = 0; i < values.length; ++i ) {
            prevValues[i] = prevValues[i] + ALPHA * (values[i] - prevValues[i]);
        }
        return prevValues;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (false == mLocationTaskStarted) {
            mCrtLocation = location;
            mTimer.scheduleAtFixedRate(mLocationTask, 0, 1000);
            mLocationTaskStarted = true;
        }
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

    public Location getCrtLocation() {
        return mCrtLocation;
    }

    public LocationData isLocationInspection(float coords[]) {
        Matrix matrix = new Matrix();
        matrix.set(getMatrix());
        matrix.preRotate(mRotateFactor);
        matrix.mapPoints(coords);

        Log.e(DEBUG_TAG, Float.toString(coords[0]) + ", " + Float.toString(coords[1]));
        for (int i = mLocations.size() - 1; i >= 0; --i) {
            LocationData ld = mLocations.get(i);
            Log.e(DEBUG_TAG, ld.mPlaceName + ": " + Float.toString(ld.mX) + ", " + Float.toString(ld.mY));
            if (ld.isInside(coords)) {
                //Toast.makeText(getContext(), ld.mPlaceName + " was clicked!", Toast.LENGTH_LONG).show();
                return ld;
            }
        }

        return null;
    }

    private void parseLocationResult(JSONObject result) {
        mDontDraw = true;
        String id, place_id, placeName = null, reference, icon, vicinity = null;
        boolean open;
        double latitude, longitude, rating;

        try {
            JSONArray jsonArray = result.getJSONArray("results");

            if (result.getString(STATUS).equalsIgnoreCase(OK)) {
                mLocations.clear();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject place = jsonArray.getJSONObject(i);

                    id = place.getString(SUPERMARKET_ID);
                    place_id = place.getString(PLACE_ID);
                    if (!place.isNull(NAME)) {
                        placeName = place.getString(NAME);
                    }
                    if (!place.isNull(VICINITY)) {
                        vicinity = place.getString(VICINITY);
                    }
                    latitude = place.getJSONObject(GEOMETRY).getJSONObject(LOCATION)
                            .getDouble(LATITUDE);
                    longitude = place.getJSONObject(GEOMETRY).getJSONObject(LOCATION)
                            .getDouble(LONGITUDE);
                    reference = place.getString(REFERENCE);
                    icon = place.getString(ICON);
                    try {
                        open = place.getJSONObject(OPENING_HOURS).getBoolean(IS_OPEN);
                    }
                    catch (JSONException e) {
                        open = false;
                    }

                    rating = place.getDouble(RATING);

                    // TODO: Create a new location
                    LocationData locationData = new LocationData(id, place_id, placeName, reference,
                            icon, vicinity, open, rating, latitude, longitude);
                    mLocations.add(locationData);
                }

                /*Toast.makeText(getContext(), jsonArray.length() + " locations found!",
                        Toast.LENGTH_LONG).show();*/
            } else if (result.getString(STATUS).equalsIgnoreCase(ZERO_RESULTS)) {
                Toast.makeText(getContext(), "No entry found in 5KM radius!!!",
                        Toast.LENGTH_LONG).show();
            }

        } catch (JSONException e) {

            e.printStackTrace();
            Log.e(DEBUG_TAG, "parseLocationResult: Error=" + e.getMessage());
        }
    }

    static Map<String, Bitmap> mBitmapsCache;
    static
    {
        mBitmapsCache = new HashMap<>();
    }

    private class ComputeLocations extends AsyncTask {
        private float cameraRotation[] = new float[9];
        private float rotation[] = new float[9];
        private float identity[] = new float[9];
        private float orientation[] = new float[3];
        private float accelerometerData[] = new float[3];
        private float compassData[] = new float[3];

        private Location locationData = null;

        private float dx, dy;

        private int width, height;

        private boolean gotRotation = false;

        ComputeLocations(int w, int h, float accelerometer[], float compass[], Location location) {
            width = w;
            height = h;

            accelerometerData = accelerometer;
            compassData = compass;

            locationData = location;
        }

        private double getDistanceFromLocation(double latitude, double longitude) {
            double crtLatitude = mCrtLocation.getLatitude();
            double crtLongitude = mCrtLocation.getLongitude();
            float latDist = (float)(crtLatitude - latitude);
            float LongDist = (float)(crtLongitude - longitude);

            return Math.sqrt(latDist * latDist + LongDist * LongDist);
        }

        private double clamp(double value, double minValue, double maxValue) {
            if (value < minValue)
                return minValue;
            else if (value > maxValue)
                return maxValue;

            return value;
        }

        @Override
        protected Object doInBackground(Object... objs) {
            if (null == locationData)
                return null;

            if (mLocations.isEmpty())
                return null;

            if (null == mCenterPos)
                return null;

            float bearing;

            // compute rotation matrix
            gotRotation = SensorManager.getRotationMatrix(rotation,
                    identity, accelerometerData, compassData);

            if (gotRotation) {
                // remap such that the camera is pointing along the positive direction of the Y axis
                SensorManager.remapCoordinateSystem(rotation, SensorManager.AXIS_X,
                        SensorManager.AXIS_Z, cameraRotation);

                SensorManager.getOrientation(cameraRotation, orientation);
                float rotFactor = (float)(0.0f - Math.toDegrees(orientation[2]));
                Log.e("Debug",  "Rotate factor: " + Float.toString(rotFactor));

                if (Math.abs(rotFactor - mRotateFactor) < 0.3f) {
                    return null;
                }
                mRotateFactor = rotFactor;


                Camera cam = cameraData.getCamera();
                if (null == cam)
                    return null;

                Camera.Parameters params = cam.getParameters();
                float verticalFOV = params.getVerticalViewAngle();
                float horizontalFOV = params.getHorizontalViewAngle();

                double minDist = Double.MAX_VALUE;
                double maxDist = Double.MIN_VALUE;
                for (LocationData ld : mLocations) {
                    double latitude = ld.mLocation.getLatitude();
                    double longitude = ld.mLocation.getLongitude();
                    double distance = getDistanceFromLocation(latitude, longitude);
                    ld.setDistance((float)distance);

                    minDist = Math.min(minDist, distance);
                    maxDist = Math.max(maxDist, distance);
                }

                for (LocationData ld : mLocations) {
                    double percent = clamp((ld.mDistance - minDist) / (maxDist - minDist), 0.2, 0.8) - 0.5;
                    //Log.e("Debug", Float.toString(ld.mDistance) + ": " + Double.toString(percent));

                    bearing = locationData.bearingTo(ld.mLocation);
                    dx = (float) ((width / horizontalFOV) * (Math.toDegrees(orientation[0]) - bearing));
                    dy = (float) (height * percent);// * Math.toDegrees(orientation[1]));
                    ld.setCoords( mCenterPos.first - dx,  mCenterPos.second - dy);

                    try {
                        if (true == mBitmapsCache.containsKey(ld.mIcon)) {
                            ld.mIconBitmap = mBitmapsCache.get(ld.mIcon);
                        } else {
                            InputStream in = new URL(ld.mIcon).openStream();
                            ld.mIconBitmap = BitmapFactory.decodeStream(in);
                            mBitmapsCache.put(ld.mIcon, ld.mIconBitmap);
                        }
                    }
                    catch (Exception e) {
                        Log.e("Error", e.getMessage());
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object obj) {
            mDontDraw = false;
        }
    }

    class DrawTask extends TimerTask {

        @Override
        public void run() {
            OverlayView.this.invalidate();
        }
    }

    class LocationsTask extends TimerTask {

        @Override
        public void run() {
            if (null == mCrtLocation)
                return;

            double latitude = mCrtLocation.getLatitude();
            double longitude = mCrtLocation.getLongitude();

            //YOU Can change this type at your own will, e.g hospital, cafe, restaurant.... and see how it all works
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

            String locationIdx = sharedPreferences.getString("filter_list", "0");
            //String value = sharedPreferences.getString(p.getKey(), "")
            String type = mLocationTypeById.get(locationIdx);

            Integer distance = sharedPreferences.getInt("distance_slider", 5);

            StringBuilder googlePlacesUrl =
                    new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
            googlePlacesUrl.append("location=").append(latitude).append(",").append(longitude);
            googlePlacesUrl.append("&radius=").append(distance * 1000);
            googlePlacesUrl.append("&types=").append(type);
            googlePlacesUrl.append("&sensor=true");
            googlePlacesUrl.append("&key=" + GOOGLE_BROWSER_API_KEY);

            JsonObjectRequest request = new JsonObjectRequest(googlePlacesUrl.toString(),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject result) {

                            Log.i(DEBUG_TAG, "onResponse: Result= " + result.toString());
                            parseLocationResult(result);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override                    public void onErrorResponse(VolleyError error) {
                            Log.e(DEBUG_TAG, "onErrorResponse: Error= " + error);
                            Log.e(DEBUG_TAG, "onErrorResponse: Error= " + error.getMessage());
                        }
                    });

            PlacesController.getInstance().addToRequestQueue(request);
        }
    }
}
