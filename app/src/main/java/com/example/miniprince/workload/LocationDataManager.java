package com.example.miniprince.workload;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.nfc.Tag;
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

import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.paperdb.Paper;

/**
 * Starts a service that can constantly monitor the user's location, update the time spent there,
 * and determine if the area has changed.
 */
public class LocationDataManager extends Service {
    private final String TAG = "ATS";

    // The listener for location updates
    private LocationListener locationListener;

    // The manager for the LocationListener
    private LocationManager locationManager;

    // A flag for whether or not the Service is currently running
    public static boolean isRunning = false;

    // Strings for accessing the extra information attached to location broadcasts to the UI
    public static final String
            ACTION_LOCATION_BROADCAST = LocationDataManager.class.getName() + "LocationBroadcast",
            EXTRA_LATITUDE = "extra_latitude",
            EXTRA_LONGITUDE = "extra_longitude",
            LOCATION_TIME = "location_time",
            SHOULD_UPDATE_LOCATION = "should_update_location",
            SHOULD_PAN = "should_pan";

    // TODO: threshold is set pretty low, probably want like 20 minutes
    public static final int
            // The minimum time a user has to spend in an area for notification to occur, in milliseconds
            MIN_TIME = 5000,

    // The minimum distance a user has to travel from their last location for a new area to be detected
    MIN_DISTANCE = 1609,

    // ID for notification
    NOTIFICATION_ID = 543;

    // The the time at which a new RecordedLocation was registered as significant
    private DateTime startTime = new DateTime();

    // The last received geographical location from the location listener
    private Location lastGeographicalLocation;

    // The current location being recorded for data
    private RecordedLocation currentRecordedLocation;

    // A flag for whether or not the user is being notified that data is being collected on a location
    private boolean isUserNotified = false;

    // A flag for whether or not the most recently received location was detected as a new area
    private boolean isAreaUpdated = false;

    // TODO: could probably create a better system for this
    // A flag for detecting if the most recently received location was the first ever detected, for UI purposes
    private boolean firstUpdate = true;

    // The database currently being accessed
    private UserData currentData;

    // The Notification manager to send info to the user
    private NotificationManager notificationManager;

    // Status for managing notifications
    private NotificationStatus status = NotificationStatus.UNREGISTERED_EVENT;

    // BroadcastReceiver to receive intent content when the user does an action on a notification
    private BroadcastReceiver nReceiver;

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
    public void onCreate() {
        Log.i(TAG, "Service Created.");
        // Initialize the location manager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Initialize the data reader
        Paper.init(this);

        // Initialize the notification manager
        initNotificationManager();

        // Initialize the notification receiver
        initBroadcastReceiver();

        // Read the current user data
        fetchUserData();
    }

    /**
     * Initializes the notification manager and channel for the service.
     */
    private void initNotificationManager() {
        Log.i(TAG, "Initializing notification manager.");
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Add notification channel for Android O
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Log.i(TAG, "Android O detected, adding notification channel.");

            String id = getString(R.string.location_channel);

            CharSequence name = getString(R.string.app_name);

            //TODO: do this with resources
            String desc = "Description";

            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(id, name, importance);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Initializes the BroadcastReceiver for notifications.
     */
    private void initBroadcastReceiver() {
        nReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case "pause.data":
                        Log.i(TAG, "Pausing data.");
                        break;
                    case "save.as.work":
                        Log.i(TAG, "Saving as work.");
                        currentRecordedLocation.setSaved(true);
                        status = NotificationStatus.SAVED_WORK;
                        // I think we have to manually store it here, or set a value for manual save from user
                        // Also need to add a situation where GPS is lost, and so things stop recording for a bit but pick back up after
                        break;
                    case "mark.as.work":
                        Log.i(TAG, "Marking as work.");
                        currentRecordedLocation.setLocationType(LocationType.WORK);
                        status = NotificationStatus.UNSAVED_WORK;
                        break;
                    default: break;
                }
                updateNotification();
            }
        };

        IntentFilter actionFilter = new IntentFilter();
        actionFilter.addAction("pause.data");
        actionFilter.addAction("save.as.work");
        actionFilter.addAction("mark.as.work");

        registerReceiver(nReceiver, actionFilter);
    }

    /**
     * Accesses the current UserData, and prepares it for further storage on the user's location.
     */
    private void fetchUserData() {
        Log.i(TAG, "Fetching user data.");

        Thread fetch = new Thread(new Runnable() {
            volatile boolean isRunning = true;

            @Override
            public void run() {
                Log.i(TAG, "Starting to write data.");

                currentData = Paper.book().read(getResources().getString(R.string.user_data));

                while (isRunning) {
                    if (currentData == null) {
                        Log.i(TAG, "Data being read.");
                    } else {
                        Log.i(TAG, "Data fetched.");
                        isRunning = false;
                    }
                }

                Log.i(TAG, "Thread ending.");
                return;
            }
        });

        fetch.start();
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

    private void startLocationService() {
        Log.i(TAG, "Starting Location services.");

        if (isRunning) {
            Log.i(TAG, "Service is already running.");
            sendBroadcastMessage(lastGeographicalLocation);
            return;
        }

        Log.i(TAG, "Service is being restarted.");

        // Notify the user the service is running
        updateNotification();

        isRunning = true;
        // Initialize the location manager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Make sure permissions to request location are granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Handled in UI
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
        Log.i(TAG, "Initializing location listener.");

        locationListener = new LocationListener() {

            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void onLocationChanged(Location location) {
                Log.i(TAG, "New Location received.");

                // Perform checks on the new location compared to the current one
                updateCurrentLocation(location, MIN_DISTANCE);

                // If a new area was detected
                if (isAreaUpdated) {
                    Log.i(TAG, "Area update detected.");

                    // Mark the old location's last updated time, and process the new location
                    initializeNewLocationData(location);

                    // Set a new start time to record from
                    resetTimeSpentInLocation(location.getTime());
                }

                // Perform checks on the status time spent at the current recorded location, and notify if necessary
                updateCollectionStatus(location, MIN_TIME);

                // broadcast the new information
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
                Toast.makeText(LocationDataManager.this, "GPS is required to show location. Redirecting to settings.",
                        Toast.LENGTH_LONG).show();
                Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                LocationDataManager.this.startActivity(settingsIntent);
            }
        };
    }

    /**
     * Updates the current location. If the current Area is null, the service has initially been
     * started and the location is updated. Otherwise, if the new location is outside a one mile
     * radius of the last location, the location is updated. If the location is updated, isAreaUpdated
     * becomes true, so that the UI may update accordingly.
     *
     * @param location the new location to judge
     * @param range    the qualifying range for a new distance, in meters
     */
    private void updateCurrentLocation(Location location, int range) {
        Log.i(TAG, "Processing new Location.");

        // Checking distance for the first time, true
        if (lastGeographicalLocation == null) {
            Log.i(TAG, "First location detected, setting new to current.");
            lastGeographicalLocation = location;
            isAreaUpdated = true;
            return;
        }

        double currLat = lastGeographicalLocation.getLatitude();
        double currLong = lastGeographicalLocation.getLongitude();

        float[] result = new float[1];

        Location.distanceBetween(currLat, currLong, location.getLatitude(), location.getLongitude(), result);

        if (result[0] > range) {
            Log.i(TAG, "New location past range threshold, no update.");
            lastGeographicalLocation = location;
            isAreaUpdated = true;
        } else {
            Log.i(TAG, "Location in same area.");
            isAreaUpdated = false;
        }
    }

    /**
     * Begins data collection by initializing a new RecordedLocation. If the user was in a previous location,
     * that information's lastTimeVisited is marked.
     */
    private void initializeNewLocationData(Location location) {
        Log.i(TAG, "Initializing RL data for new location.");

        // If this isn't the first RecordedLocation since starting
        if (currentRecordedLocation != null) {
            Log.i(TAG, "Setting lastTimeVisited for old location.");

            // Set the last visited time to the current time
            Interval startFinish = new Interval(startTime, new DateTime());
            currentRecordedLocation.addInterval(startFinish);
        }


        // Check if the new location is a previously visited work location
        RecordedLocation saved = currentData.searchForPastWorkLocation(location);

        // If the search was successful, set the currentRecordedLocation to the found location
        if (saved != null) {
            Log.i(TAG, "Saved work RL found, setting.");

            currentRecordedLocation = saved;
        } else {
            // The search was not successful, check that it's a previously visited OTHER location
            RecordedLocation unsaved = currentData.searchForPastOtherLocation(location);

            // If the search was successful, set the currentRecordedLoction to the found location
            if (unsaved != null) {
                Log.i(TAG, "Unsaved work RL found, setting.");

                currentRecordedLocation = unsaved;
            }

            // If not, create a new RecordedLocation, set status to OTHER, set it to current
            else {
                Log.i(TAG, "Novel location detected, setting.");

                Geocoder coder = new Geocoder(this);

                try {
                    currentRecordedLocation = new RecordedLocation(
                            location.getLatitude(),
                            location.getLongitude(),
                            LocationType.OTHER,
                            false,
                            coder.getFromLocation(
                                    location.getLatitude(),
                                    location.getLongitude(),
                                    1).get(0).getFeatureName());
                } catch (IOException e) {
                    Log.e(TAG, "COULDN'T RECORD LOCATION");
                }
            }
        }

        // Turn off the notification, and note that the user is not being notified about anything significant
        status = NotificationStatus.UNREGISTERED_EVENT;
        updateNotification();
    }

    /**
     * Updates the notification telling the user information about a certain tracking situation,
     * based on the context of the currentRecordedLoction.
     */
    private void updateNotification() {
        Log.i(TAG, "Updating notification about location.");

        Intent notificationIntent = new Intent(getApplicationContext(), CurrentArea.class);
        notificationIntent.setAction("Location Managing");  // A string containing the action name
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        NotificationCompat.Builder notificationBuilder;

        // Check OS version
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationBuilder = new NotificationCompat.Builder(this, getString(R.string.location_channel))
                    .setContentTitle(getResources().getString(R.string.app_name))
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setContentIntent(contentPendingIntent)
                    .setOngoing(true);
        } else {
            notificationBuilder = new NotificationCompat.Builder(this)
                    .setContentTitle(getResources().getString(R.string.app_name))
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setContentIntent(contentPendingIntent)
                    .setOngoing(true);
        }

        switch (status) {
            case SAVED_WORK:
                Intent pIntent = new Intent("pause.data");
                PendingIntent pause = PendingIntent.getBroadcast(this, 4, pIntent, PendingIntent.FLAG_CANCEL_CURRENT);

                Log.i(TAG, "Setting notification for saved Work RL.");
                notificationBuilder.setContentText("Monitoring saved location.");
                notificationBuilder.addAction(R.mipmap.ic_launcher_round, "PAUSE", pause);
                break;

            case UNSAVED_WORK:
                Intent sIntent = new Intent("save.as.work");
                PendingIntent save = PendingIntent.getBroadcast(this, 4, sIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                pIntent = new Intent("pause.data");
                pause = PendingIntent.getBroadcast(this, 4, pIntent, PendingIntent.FLAG_CANCEL_CURRENT);

                Log.i(TAG, "Setting notification for unsaved Work RL.");
                notificationBuilder.setContentText("Monitoring work location.");
                notificationBuilder.addAction(R.mipmap.ic_launcher_round, "SAVE", save);
                notificationBuilder.addAction(R.mipmap.ic_launcher_round, "PAUSE", pause);
                break;

            case OTHER_LOCATION:
                Intent mIntent = new Intent("mark.as.work");
                PendingIntent mark = PendingIntent.getBroadcast(this, 4, mIntent, PendingIntent.FLAG_CANCEL_CURRENT);

                Log.i(TAG, "Setting notification for unknown RL.");
                notificationBuilder.setContentText("Monitoring unknown location.");
                notificationBuilder.addAction(R.mipmap.ic_launcher_round, "MARK AS WORK", mark);
                break;

            case UNREGISTERED_EVENT:
                Log.i(TAG, "Setting neutral notification.");
                notificationBuilder.setContentText("Running location service.");
                break;
            default:
                break;
        }

        Notification notification = notificationBuilder.build();

        notification.flags = notification.flags | Notification.FLAG_NO_CLEAR;

        if (!isRunning) {
            startForeground(NOTIFICATION_ID, notification);
        } else {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    /**
     * Resets the count of how long the user has spent in the current area.
     */
    private void resetTimeSpentInLocation(long millis) {
        Log.i(TAG, "Resetting time spent in Location.");

        startTime = new DateTime(millis);
    }

    /**
     * Initially checks if the current location has been visited for the given amount of time.
     * If so, if the user has not already been notified, launches a notification with details
     * pertaining to the information about the current location, how the application is collecting
     * data, and finally any actions that the user  may want to take about the location, such as
     * recording it as a certain type, or pausing any automatic data collection. Additionally, if
     * the currentRecordedLocation is newly created, it will be stored in the database.
     *
     * @param location the location to examine
     */
    private void updateCollectionStatus(Location location, long minTime) {
        Log.i(TAG, "Updating data collection status.");

        /* If the user is not currently being notified about their location and they've spent enough
           time in the given location*/
        if (getTimeSinceLocation(location.getTime()) >= minTime) {
            Log.i(TAG, "Time threshold met, checking for notification.");

            // If the user has not been notified already
            if (status == NotificationStatus.UNREGISTERED_EVENT) {
                Log.i(TAG, "User is not being notified.");

                // If the event is newly created, store it in the correct database
                if (currentRecordedLocation.isNewlyCreated()) {
                    currentData.storeNewLocation(currentRecordedLocation);
                }

                // Update the status of the current location
                if (currentRecordedLocation.getType() == LocationType.WORK) {
                    if (currentRecordedLocation.isSaved()) {
                        status = NotificationStatus.SAVED_WORK;
                    } else {
                        status = NotificationStatus.UNSAVED_WORK;
                    }
                } else {
                    status = NotificationStatus.OTHER_LOCATION;
                }

                // Pop the notification
                updateNotification();
            }
        } else {
            Log.i(TAG, "Time threshold not met, current duration: " + getTimeSinceLocation(location.getTime()));

        }
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
     * Determines the amount of time spent in the current location.
     *
     * @param millis The timestamp of the most recently fetched location.
     * @return the amount fo time spent in the current location.
     */
    private long getTimeSinceLocation(long millis) {
        return millis - startTime.getMillis();
    }

}

