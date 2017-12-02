package com.example.miniprince.workload;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.List;

public class CurrentArea extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private LocationManager locationManager;

    private LocationListener locationListener;

    private static String TAG = "Current Area";

    private Marker currentLocation;

    private long startMinutes = 0;
    private long startHours = 0;

    private TextView timeView;

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
        updateTimeSpentInLocation(0);

        // Initialize the locationListener
        initLocationListener();

        // Initialize the location manager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Make sure permissions to request location are granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        // TODO: If the NETWORK_PROVIDER is not available, make the GPS_PROVIDER an option
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
                // Get the latitude and longitude for address conversion
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                LatLng latLng = new LatLng(latitude, longitude);

                // Instantiate the Geocoder to convert latLng into an address.
                Geocoder geocoder = new Geocoder(getApplicationContext());

                try {
                   List<Address> results = geocoder.getFromLocation(latitude, longitude, 1);
                   String addressLine = results.get(0).getAddressLine(0);

                   // Check that the user hasn't greatly moved, and if they have
                    // TODO: Figure out how to properly name this function, it really doesn't make sense with how it's being used
                   if (isNewArea(currentLocation, latLng)) {
                       // Move the map and camera to the current location.
                       currentLocation = mMap.addMarker(new MarkerOptions().position(latLng).title(addressLine));
                       mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20.2f));
                       resetTimeSpentInLocation(location.getTime());
                   }

                   updateTimeSpentInLocation(location.getTime());

                } catch (IOException e) {
                    // Let the user know we couldn't get anything.
                    Toast.makeText(CurrentArea.this,
                            "Couldn't get information from this current location.",
                            Toast.LENGTH_SHORT);
                }
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            // TODO: Add a dialogue box that prompts the user that they need to enable gps, and if so starts the intent
            @Override
            public void onProviderDisabled(String s) {
                Toast.makeText(CurrentArea.this, "GPS is required to show location. Redirecting to settings.",
                        Toast.LENGTH_LONG).show();
                 Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                 CurrentArea.this.startActivity(settingsIntent);
            }
        };
    }

    /**
     * Updates the display of how long the user has spent in the current area.
     */
    private void updateTimeSpentInLocation(long millis) {
        long minute = (millis / (1000 * 60)) % 60;
        long hour = (millis / (1000 * 60 * 60)) % 24;

        long currHours = hour - startHours;
        long currMinutes = minute - startMinutes;

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
     * Resets the display of how long the user has spent in the current area.
     */
    private void resetTimeSpentInLocation(long millis) {
        long minute = (millis / (1000 * 60)) % 60;
        long hour = (millis / (1000 * 60 * 60)) % 24;

        startMinutes = minute;
        startHours = hour;

        timeView.setText(minute - startMinutes + " Minutes");
    }


    /**
     * Determines if the current area is outside a range of 1 mile, to the given LatLng.
     * @param current the current location
     * @param newLocation the new location
     * @return true if the range is large enough
     */
    private boolean isNewArea(Marker current, LatLng newLocation) {
        // Checking distance for the first time, true
        if (currentLocation == null) {
            return true;
        }

        LatLng currLatLng = current.getPosition();
        double currLat = currLatLng.latitude;
        double currLong = currLatLng.longitude;

        float[] result = new float[1];

        Location.distanceBetween(currLat, currLong, newLocation.latitude, newLocation.longitude, result);

        // Result is returned in meters, 1mi = ~1609 meters
        return result[0] > 1610;
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
}
