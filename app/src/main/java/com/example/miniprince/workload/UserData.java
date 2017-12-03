package com.example.miniprince.workload;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * Class that stores all data about the user. This includes the user's name, their saved location
 * history, and the distribution data for how much time was spent working by the user.
 *
 * Data is stored on a year to year basis. Any SavedLocations that have creation times equal to a
 * year behind the current system time will be marked as outdated information, and removed.
 */

public class UserData implements Serializable {
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

    /*
    On first run of the app:
    - Prompt the user with the "choose your distribution" screen
    - Once that's selected, create a new UserData object with the given preferences.
     */

    /*
    We have a new location. (some of this is handled in the data storage service)
    - First wait for like 20 minutes to see if they settle
    - If they keep moving around, don't save the locations
    - If they do settle, look at the LatLng of the area, and search the savedLocations for an instance

    If the instance is found:
    - Set the current RecordedLocation being updated to the found instance
    - Pop a notification saying that the work is being recorded in a saved location. Give them the option to
      pause recording time in that place.

    If the instance is not found:
    - Detect it as a new area, and create a new RecordedLocation, initially marked as OTHER.
    - Pop a notification letting them know that the system noticed they've settled, and that
      it's recording their time spent there. Options: Mark as Work | Save Location
    - Once the notification is popped, check

    In the case that they mark it as work:
    - set the LocationType to WORK
    - save the object in allWorkLocations
    - save the UUID in unsavedLocations
    - every time an update is received, increment the totalTimeWorked for the location

    In the case that they save the location:
    - set the LocationType to WORK
    - save the object in allWorkLocations
    - save the UUID in savedLocations
    - every time an update is received, increment the totalTimeWorked for the location

    In the case they do nothing about it, and then leave:
    - every time an update is received, increment the totalTimeWorked for the location
    - save the object in allUnmarkedLocations
     */
}
