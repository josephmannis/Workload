package com.example.miniprince.workload;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

/**
 * Starts a service that can constantly monitor the user's location, update the time spent there,
 * and determine if the area has changed.
 */

public class AreaTimerService extends Service {
    private final String TAG = "ATS";
    private LocationListener locationListener;
    private LocationManager locationManager;
    public static boolean isRunning = false;

    public static final String
            ACTION_LOCATION_BROADCAST = AreaTimerService.class.getName() + "LocationBroadcast",
            EXTRA_LATITUDE = "extra_latitude",
            EXTRA_LONGITUDE = "extra_longitude",
            LOCATION_TIME = "location_time",
            SHOULD_UPDATE_LOCATION = "should_update_location",
            SHOULD_PAN = "should_pan";

    private static final int
            MIN_TIME = 2000,
            MIN_DISTANCE = 1609,
            NOTIFICATION_ID = 543;

    private long startTime = 0;

    private Location lastLocation;

    private boolean isAreaUpdated = false;

    private boolean firstUpdate = true;


    class MapServiceBinder extends Binder {
        public MapServiceBinder getService() {
            return MapServiceBinder.this;
        }
    }

    private IBinder mBinder = new MapServiceBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind initiated.");
        return null;
    }

    @Override
    public void onStart(Intent intent, int startID) {
        Log.i(TAG, "Service started.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onsStartCommand initiated.");

        startLocationService();
        return START_STICKY;
    }

    // TODO: Create a notification for the user while the map is updating
    private void startLocationService() {
        if (isRunning) {
            Log.i(TAG, "Service is already running.");
            sendBroadcastMessage(lastLocation);
            return;
        }

        Log.i(TAG, "Service is being restarted.");


        isRunning = true;
        // Initialize the location manager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Make sure permissions to request location are granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Handled in UI
        }

        // TODO: Make this work lol
        Intent notificationIntent = new Intent(getApplicationContext(), CurrentArea.class);
        notificationIntent.setAction("Location Managing");  // A string containing the action name
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        //   Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.my_icon);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setTicker(getResources().getString(R.string.app_name))
                .setContentText("Running activity")
                //     .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(contentPendingIntent)
                .setOngoing(true)
//                .setDeleteIntent(contentPendingIntent)  // if needed
                .build();
        notification.flags = notification.flags | Notification.FLAG_NO_CLEAR;     // NO_CLEAR makes the notification stay when the user performs a "delete all" command
        startForeground(NOTIFICATION_ID, notification);

        initLocationListener();
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy initiated.");

        super.onDestroy();
    }

    @Override
    public void onCreate() {
        // Initialize the location manager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Make sure permissions to request location are granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Handled in UI
            return;
        }

        initLocationListener();
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }

    /**
     * This initializes the location listener used to manipulate the marker on the map. The location
     * listener can also be used to update the necessary data about how much time was spent in an
     * area based on the changes to the user's location.
     */
    private void initLocationListener() {
        locationListener = new LocationListener() {

            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void onLocationChanged(Location location) {
                updateCurrentLocation(location, MIN_DISTANCE);

                if (isAreaUpdated) {
                    resetTimeSpentInLocation(location.getTime());
                }

                sendBroadcastMessage(location);
                firstUpdate = false;
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            // TODO: Add a dialogue box that prompts the user that they need to enable gps, and if so starts the intent, or just more gracefully handle this
            @Override
            public void onProviderDisabled(String s) {
                //TODO: possibly make the actual interface handle this
                Toast.makeText(AreaTimerService.this, "GPS is required to show location. Redirecting to settings.",
                        Toast.LENGTH_LONG).show();
                Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                AreaTimerService.this.startActivity(settingsIntent);
            }
        };
    }

    /**
     * Sends the new latitude and longitude
     *
     * @param location
     */
    private void sendBroadcastMessage(Location location) {
        if (location != null) {
            Log.i(TAG, "Broadcasting location.");
            Intent intent = new Intent(ACTION_LOCATION_BROADCAST);
            intent.putExtra(EXTRA_LATITUDE, location.getLatitude());
            intent.putExtra(EXTRA_LONGITUDE, location.getLongitude());
            intent.putExtra(LOCATION_TIME, getTimeSinceLocation(location.getTime()));
            intent.putExtra(SHOULD_UPDATE_LOCATION, isAreaUpdated);
            intent.putExtra(SHOULD_PAN, firstUpdate);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    /**
     * Resets the display of how long the user has spent in the current area.
     */
    private void resetTimeSpentInLocation(long millis) {
        startTime = millis;
    }

    /**
     * Updates the current location. If the current Area is null, the service has initially been
     * started and the location is updated. Otherwise, if the new location is outside a one mile
     * radius of the last location, the location is updated. If the location is updated, isAreaUpdated
     * becomes true, so that the UI may update accordingly.
     * @param location the new location to judge
     * @param range the qualifying range for a new distance, in meters
     */
    private void updateCurrentLocation(Location location, int range) {
        // Checking distance for the first time, true
        if (lastLocation == null) {
            lastLocation = location;
            isAreaUpdated = true;
            return;
        }

        double currLat = lastLocation.getLatitude();
        double currLong = lastLocation.getLongitude();

        float[] result = new float[1];

        Location.distanceBetween(currLat, currLong, location.getLatitude(), location.getLongitude(), result);

        // Result is returned in meters, 1mi = ~1609 meters
        if (result[0] > range) {
            lastLocation = location;
            isAreaUpdated = true;
        } else {
            isAreaUpdated = false;
        }
    }

    /**
     * Determines the amount of time spent in the current location.
     * @param millis The timestamp of the most recently fetched location.
     * @return the amount fo time spent in the current location.
     */
    private long getTimeSinceLocation(long millis) {
        return millis - startTime;
    }

}

