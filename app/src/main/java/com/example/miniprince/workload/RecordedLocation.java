package com.example.miniprince.workload;

import android.location.Location;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;

import java.io.Serializable;
import java.time.Year;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

/**
 * Representation of a recorded location. These locations are created when the user enters a new area
 * and remains in that area for enough time to be deemed significant.
 */
public class RecordedLocation implements Serializable {

    private double latitude;
    private double longitude;
    private LocationType type;
    private long totalTimeSpent; // In milliseconds
    private final UUID id;
    private boolean isSaved;
    private boolean isNewlyCreated;
    private ArrayList<Interval> timesVisited;
    private String title;

    /**
     * Creates a new SavedLocation at the given latitude and longtiude, and with the type.
     * @param latitude the latitude of this SavedLocation's location
     * @param longitude the longitude of this SavedLocation's location
     * @param type the type of this SavedLocation
     * @param isSaved displays whether or not the user has saved this location
     */
    public RecordedLocation(double latitude, double longitude, LocationType type, boolean isSaved, String title) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
        this.totalTimeSpent = 0;
        this.id = UUID.randomUUID();
        this.isSaved = isSaved;
        this.isNewlyCreated = true;
        this.timesVisited = new ArrayList<Interval>();
        this.title = title;
    }

    /**
     * Determines if the given location is within the given range of this SavedLocation's latitude
     * and longitude
     * @param l the location to be compared
     * @param distance the qualifying range
     * @return the flag for whether or not the given location is within range
     */
    public boolean isWithinRange(Location l, int distance) {
        float[] result = new float[1];

        Location.distanceBetween(latitude, longitude, l.getLatitude(), l.getLongitude(), result);

        // Result is returned in meters, 1mi = ~1609 meters
        return (result[0] > distance);
    }

    /**
     * Gets the LocationType for this SavedLocation.
     */
    public LocationType getType() {
        return type;
    }

    /**
     * Sets the LocationType for this SavedLocation
     * @param type
     */
    public void setLocationType(LocationType type) {
        this.type = type;
    }

    /**
     * @return the total amount of time spent at this SavedLocation.
     */
    public long getTotalTimeSpent() {

        return totalTimeSpent;
    }

    /**
     * Adds the given value of time to the total time of this SavedLocation.
     * @param millis the amount of time to increment, in milliseconds
     */
    public void updateTotalTimeSpent(long millis) {
        this.totalTimeSpent += millis;
    }

    /**
     * Gets the UUID for this SavedLocation.
     */
    public UUID getId() {
        return id;
    }

    /**
     * @return true if this RecordedLocation is saved
     */
    public boolean isSaved() {
        return this.isSaved;
    }

    /**
     * Sets the saved status of this RecordedLocation
     * @param b value to set
     */
    public void setSaved(boolean b) {
        this.isSaved = b;
    }


    /**
     * Determines if this RecordedLoction is a newly created location, that is, it is not already
     * stored everywhere.
     */
    public boolean isNewlyCreated() {
        return this.isNewlyCreated;
    }

    /**
     * Sets the status of this RecordedLoction's creation status.
     */
    public void setNewlyCreated(boolean b) {
        this.isNewlyCreated = b;
    }

    /**
     * Adds an interval to this RecordedLocation and updates the totalTimeSpent.
     */
    public void addInterval(Interval i) {
        this.timesVisited.add(i);
        totalTimeSpent += i.toDurationMillis();
    }

    /**
     * Gets the amount of time this RecordedLocation was visited after the given DateTime.
     * @param threshold the DateTime from which to check
     * @return the total time visited in the range
     */
    public long getTimeVisitedInRange(DateTime threshold) {
        long totalTime = 0;

        for (Interval i : timesVisited) {
            if (i.getStart().getMillis() >= threshold.getMillis()) {
                totalTime += i.toDurationMillis();
            }
        }

        return totalTime;
    }

    /**
     * Determines if this event has been visited in the given range.
     */
    public boolean isVisitedInRange(DateTime threshold) {
        for (Interval i : timesVisited) {
            if (i.getStart().getMillis() >= threshold.getMillis()) {
                return true;
            }
        }
     return false;
    }


    /**
     * Gets the title of this RecordedLocation. If the title is null, it means it
     * was created at an address with no recognizable places around it.
     */
    public String getTitle() {
        if (this.title == null) {
            return "Unknown.";
        }

        return title;
    }
}
