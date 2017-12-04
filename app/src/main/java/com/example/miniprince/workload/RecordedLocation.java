package com.example.miniprince.workload;

import android.location.Location;

import java.io.Serializable;
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
    private long lastTimeVisited; // In milliseconds
    private final UUID id;
    private long creationDate; // In milliseconds
    private boolean isSaved;
    private boolean isNewlyCreated;

    /**
     * Creates a new SavedLocation at the given latitude and longtiude, and with the type.
     * @param latitude the latitude of this SavedLocation's location
     * @param longitude the longitude of this SavedLocation's location
     * @param type the type of this SavedLocation
     * @param creationDate the point at which this RecordedLocation was initialized, in milliseconds
     * @param isSaved displays whether or not the user has saved this location
     */
    public RecordedLocation(double latitude, double longitude, LocationType type, long creationDate, boolean isSaved) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
        this.totalTimeSpent = 0;
        this.id = UUID.randomUUID();
        this.creationDate = creationDate;
        this.lastTimeVisited = creationDate;
        this.isSaved = isSaved;
        this.isNewlyCreated = true;
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
     * Sets the last time this RecordedLocation was visited
     * @param millis the time to set
     */
    public void setLastTimeVisited(long millis) {
        this.lastTimeVisited = millis;
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
        this.isNewlyCreated = b;d
    }
}
