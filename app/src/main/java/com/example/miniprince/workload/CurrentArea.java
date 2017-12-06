package com.example.miniprince.workload;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.List;

public class CurrentArea extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private static String TAG = "Current Area";

    private boolean activityResumed = false;

    private boolean firstUpdate = false;

    private BroadcastReceiver broadcastReceiver;

    private TextView timeView;

    private TextView areaTitle;

    private TextView locationStatus;

    private Button currentActionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_area);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Set the time view
        timeView = findViewById(R.id.time_field);

        // Set the area title
        areaTitle = findViewById(R.id.location_title);

        // Set the location status
        locationStatus = findViewById(R.id.location_status);

        // Set the actionButton
        currentActionButton = findViewById(R.id.area_action_button);

        initOnClickListener();

        // Make sure permissions to request location are granted
        if (!locationPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        // Initialize the receiver for the location service.
        initBroadcastReceiver();

        // Initialize the broadcast manager with the receiver
        LocalBroadcastManager
                .getInstance(this)
                .registerReceiver(broadcastReceiver,
                        new IntentFilter(LocationDataManager.ACTION_LOCATION_BROADCAST));

        // TODO: If the NETWORK_PROVIDER is not available, make the GPS_PROVIDER an option
    }

    /**
     * Determines if Location Permissions were granted by the user, returns true if so.
     */
    private boolean locationPermissionsGranted() {
      return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
              == PackageManager.PERMISSION_GRANTED
              && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
              == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Initializes the BroadcastReceiver to receive updates from the LocationDataManager
     */
    private void initBroadcastReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                double latitude = intent.getDoubleExtra(LocationDataManager.EXTRA_LATITUDE, 0);
                double longitude = intent.getDoubleExtra(LocationDataManager.EXTRA_LONGITUDE, 0);
                long currTime = intent.getLongExtra(LocationDataManager.LOCATION_TIME, 0);
                boolean shouldUpdateMap = intent.getBooleanExtra(LocationDataManager.SHOULD_UPDATE_LOCATION, false);
                boolean firstUpdate = intent.getBooleanExtra(LocationDataManager.SHOULD_PAN, false);
                String title = intent.getStringExtra(LocationDataManager.AREA_NAME);
                boolean status = intent.getBooleanExtra(LocationDataManager.SAVED_LOCATION, false);

                if (firstUpdate) {
                    setUpdateStatus(firstUpdate);
                }

                updateTime(currTime);
                updateText(title, status);

                if (shouldUpdateMap || activityResumed) {
                    updateMap(latitude, longitude);
                    resetButton(status);
                    activityResumed = false;
                } else {
                    Log.i(TAG, "Location unchanged, UI remaining the same.");
                }
            }
        };
    }

    /**
     * Updates the status of how many times the map has been updated.
     */
    private void setUpdateStatus(boolean status) {
        firstUpdate = status;
    }

    @Override
    protected void onResume() {
        super.onResume();
            Log.i(TAG, "Starting Service from UI.");
            activityResumed = true;

            if (locationPermissionsGranted()) {
                startService(new Intent(this, LocationDataManager.class));
            }
    }

    /**
     * This is used to handle results from a request for certain permissions. In this case, the
     * only permission required is to use the user's location, and if that permission is not
     * granted, the user is told that the app becomes virtually useless.
     * @param requestCode the result of the request
     * @param permissions the permissions requested
     * @param grantResults the permissions granted
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == 1) {
            // Check for cancelled request
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, no action needed.
                return;
            }

            // Permission denied, notify the user.
            // TODO: Make it so a notification appears instead prompting the user with this, and asking them if they want to grant them after all
            else {
                Toast.makeText(this,
                        "Location permission denied/cancelled. Cannot determine locations!",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Updates the view portraying how much time has been spent in a location.
     */
    // TODO: Change the layout so that the textbox of the timeView remains the same for better performance
    private void updateTime(long millis) {
        Log.i(TAG, "Updating time.");
        long currMinutes = (millis / (1000 * 60)) % 60;
        long currHours = (millis / (1000 * 60 * 60)) % 24;

        // TODO: Better comparison for longs
        if (currHours <= .00001) {
            timeView.setText(currMinutes + " Minutes");
            return;
        }

        if (currMinutes <= .00001) {
            timeView.setText(currHours + " Hours");
            return;
        }

        timeView.setText(currHours + ":" + currMinutes);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }


    /**
     * Updates the map by asking it to pan to the new location of the new marker.
     * @param newLat
     * @param newLong
     */
    private void updateMap(double newLat, double newLong) {
        Log.i(TAG, "Updating Map.");
        // Instantiate the Geocoder to convert latLng into an address.
        Geocoder geocoder = new Geocoder(getApplicationContext());

        LatLng latLng = new LatLng(newLat, newLong);

        // TODO: Maybe want to move the updateTimeSpentINLocation outside of the try catch block
        try {
            List<Address> results = geocoder.getFromLocation(newLat, newLong, 1);
            String addressLine = results.get(0).getAddressLine(0);

                // Move the map and camera to the current location.
                mMap.addMarker(new MarkerOptions().position(latLng).title(addressLine));
                if (firstUpdate) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20.2f));
                    setUpdateStatus(false);
                } else {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20.2f));
                }
        }
        catch (IOException e) {
            // Let the user know we couldn't get anything.
        }
    }

    /**
     * Updates the textView notifying the user what type of location is currently being visited.
     */
    private void updateText(String title, boolean saved) {
        areaTitle.setText(title);
        Log.i(TAG, title);
        if (saved) {
            locationStatus.setText("Saved Location");
        } else {
            locationStatus.setText("Unsaved Location");
        }
    }

    /**
     * Initializes the onclicklistener for the action button
     */
    private void initOnClickListener() {
        this.currentActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String curr = (String) currentActionButton.getText();

                switch (curr) {
                    case "MARK AS WORK":
                        Intent mark = new Intent("mark.as.work");
                        sendBroadcast(mark);
                        currentActionButton.setText("SAVE AS WORK");
                        break;
                    case "SAVE AS WORK":
                        Intent save = new Intent("save.as.work");
                        sendBroadcast(save);
                        currentActionButton.setVisibility(View.GONE);
                        break;
                }

            }
        });
    }

    /**
     * Updates the button based on new locations.
     */
    private void resetButton(boolean isSaved) {
        if (!isSaved) {
            currentActionButton.setText("MARK AS WORK");
            currentActionButton.setVisibility(View.VISIBLE);
        }
    }
}
