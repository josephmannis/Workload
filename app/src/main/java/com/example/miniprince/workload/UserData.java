package com.example.miniprince.workload;

import android.location.Location;
import android.util.Log;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Class that stores all data about the user. This includes the user's name, their saved location
 * history, and the distribution data for how much time was spent working by the user.
 *
 * Data is stored on a year to year basis. Any SavedLocations that have creation times equal to a
 * year behind the current system time will be marked as outdated information, and removed.
 */

public class UserData implements Serializable {
    private final static String TAG = "UD";

    // A list of all of the recorded work locations, from the past year.
    private HashMap<UUID, RecordedLocation> allWorkLocations;

    // A list of all recorded locations that are unmarked, from the past day.
    private HashMap<UUID, RecordedLocation> allUnmarkedLocations;

    // A list of references to work locations the user has marked as saved.
    private ArrayList<UUID> savedLocations;

    // A list of all work locations that have been visited.
    private ArrayList<UUID> unsavedLocations;

    // Value that stores the time the user began to collect data on the distribution, in milliseconds.
    private long pointOfCreation;

    // Value that stores all of the time ever worked by the user, in milliseconds.
    private long totalTimeWorked;

    // Value that stores all of the time worked this past week by the user, in milliseconds.
    private long timeWorkedThisWeek;

    // Value that stores all of the time worked this past day by the user, in milliseconds.
    private long timeWorkedThisDay;

    // Ideal number of hours the user would like to be working per week, in milliseconds.
    private long idealTimePerWeek = -1;

    public UserData(long idealTimePerWeek) {
        this.allWorkLocations = new HashMap<>();
        this.allUnmarkedLocations = new HashMap<>();
        this.savedLocations = new ArrayList<>();
        this.unsavedLocations = new ArrayList<>();
        this.pointOfCreation = System.currentTimeMillis();
        this.totalTimeWorked = 0;
        this.timeWorkedThisWeek = 0;
        this.timeWorkedThisDay = 0;
        this.idealTimePerWeek = idealTimePerWeek;
    }

    @Override
    public String toString() {
        return Long.toString(idealTimePerWeek);
    }

    /**
     * Gets the ideal time the user would like to work per week, in millis.
     */
    public long getIdealTimePerWeek() {
        return idealTimePerWeek;
    }

    /**
     * Determines if the given location is within range of any work locations in this user's history.
     * @param location the location to examine
     * @return true if the location is found
     * @return null if the location is not found
     */
    public RecordedLocation searchForPastWorkLocation(Location location) {
        for (RecordedLocation l : allWorkLocations.values()) {

            if (l.isWithinRange(location, LocationDataManager.MIN_DISTANCE)) {
                return l;
            }
        }
        return null;
    }

    /**
     * Determines if the given location is within range of any OTHER locations in this user's history.
     * @param location the location to examine
     * @return true if the location is found
     * @return null if the location is not found
     */
    public RecordedLocation searchForPastOtherLocation(Location location) {
        for (RecordedLocation l : allUnmarkedLocations.values()) {

            if (l.isWithinRange(location, LocationDataManager.MIN_DISTANCE)) {
                return l;
            }
        }
        return null;
    }

    /**
     * Determines the type of a RecordedLocation and stores it in the corresponding location.
     * @throws IllegalArgumentException if the RecordedLocation passed is already in the database
     */
    public void storeNewLocation(RecordedLocation r) {
        if (!r.isNewlyCreated()) {
            throw new IllegalArgumentException("RecordedLocation must be newly created.");
        }

        // Set it as no longer newly created
        r.setNewlyCreated(false);

        // Add it to the total time worked
        totalTimeWorked += r.getTotalTimeSpent();

        // If it's an OTHER, place it into unmarked
        if (r.getType() == LocationType.OTHER) {
            Log.i(TAG, "Storing OTHER location.");
            allUnmarkedLocations.put(r.getId(), r);
            return;
        }

        // Otherwise, check if it's saved or unsaved work
        allWorkLocations.put(r.getId(), r);

        if (r.isSaved()) {
            Log.i(TAG, "Storing saved work location.");

            savedLocations.add(r.getId());
        } else {
            Log.i(TAG, "Storing unsaved work location.");

            unsavedLocations.add(r.getId());
        }
    }

    /**
     * Determines the total type worked in the given category of RecordedLocations.
     * @param type the category of RecordedLocations to search through
     * @param range the date at which a location must start after to be considered
     * @return the total time for that category in the given range
     */
    public long totalTimeWorked(PieChartCreator.DataType type, DateTime range) {
        switch (type) {
            case IDEAL:
                return getIdealTimePerWeek();
            case TOTAL:
                return getTotalTimeWorkedInArea(allWorkLocations, new ArrayList<UUID>(allWorkLocations.keySet()), range);
            case WORK_SAVED:
                return getTotalTimeWorkedInArea(allWorkLocations, savedLocations, range);
            case WORK_UNSAVED:
                return getTotalTimeWorkedInArea(allWorkLocations, unsavedLocations, range);
            default:
                throw new IllegalStateException("No recognized data type.");
        }
    }

    /**
     * Gets the total type worked in the given category of RecordedLocations. Helper for totalTimeWorked
     * @param source the map to search for ids in
     * @param keys the ids of all relevant RecordedLocations
     * @param range the threshold for a RecordedLocation to be considered
     * @return the total time spent based on the given conditions
     */
    private long getTotalTimeWorkedInArea(HashMap<UUID, RecordedLocation> source, List<UUID> keys, DateTime range) {
        long total = 0;

        for (UUID id : keys) {
            total += source.get(id).getTimeVisitedInRange(range);
        }

        return total;
    }
}
