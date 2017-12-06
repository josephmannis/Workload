package com.example.miniprince.workload;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Interval;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.paperdb.Paper;

/**
 * Generates PieCharts based on the current user data, and the specified type.
 */

public class PieChartCreator {
    // Data to draw from
    private UserData userData;

    public enum DataType {
        TOTAL, WORK_SAVED, WORK_UNSAVED

    }

    public enum Range {
        CURRENT_DAY(86400000f),
        CURRENT_WEEK(604800000f),
        CURRENT_MONTH(2419200000f),
        CURRENT_YEAR(31536000000f);

        private final float val; // time in ms
        Range(float val) {
            this.val = val;
        }

        float getVal() {
            return val;
        }
    }

    /**
     * Generates a PieChart with data of the given type in the given range.
     * @param type the type of data to be considered
     * @param range the range of time in which to consider data
     * @return a PieChart including data of the given type in the given range
     */
    public PieDataSet generateGeneralDistribution(DataType type, Range range) {
        userData = Paper.book().read("user_data");

        DateTime startThreshold = getStartOfRange(range);
        float timeSpent = userData.totalTimeWorked(type, startThreshold);

        float timeInRange = range.getVal();

        if (timeSpent != 0) {
            timeSpent = (1 - (timeSpent / timeInRange)) * 100;
        }

        float otherPercentage = 100 - timeSpent;

        // Create entry for total time worked
        PieEntry hoursWorked = new PieEntry(timeSpent, "Work");

        // Create entry for the rest of the time
        PieEntry hoursOther = new PieEntry(otherPercentage, "Other");

        List<PieEntry> entries = new ArrayList<>();
        entries.add(hoursWorked);
        entries.add(hoursOther);

        return new PieDataSet(entries, "Current Balance");
    }

    /**
     * Generates data for the user's ideal distribution in the given range.
     * @param range
     * @return
     */
    public PieDataSet generateIdealDistribution(Range range) {
        userData = Paper.book().read("user_data");

        float timeSpent = userData.getIdealTimeInRange(range);

        float timeInRange = range.getVal();

        if (timeSpent != 0) {
            timeSpent = (1 - (timeSpent / timeInRange)) * 100;
        }

        float otherPercentage = 100 - timeSpent;

        // Create entry for total time worked
        PieEntry hoursWorked = new PieEntry(timeSpent, "Work");

        // Create entry for the rest of the time
        PieEntry hoursOther = new PieEntry(otherPercentage, "Other");

        List<PieEntry> entries = new ArrayList<>();
        entries.add(hoursWorked);
        entries.add(hoursOther);

        return new PieDataSet(entries, "Current Balance");

    }

    /**
     * Generates the corresponding range to the given type.
     * @param range the range type
     * @return the relevant Interval
     */
    public DateTime getStartOfRange(Range range) {
        switch (range) {
            case CURRENT_DAY:
                return new DateTime(LocalDate.now().toDateTimeAtStartOfDay());
            case CURRENT_WEEK:
                return new DateTime(LocalDate.now().withDayOfWeek(DateTimeConstants.MONDAY).toDateTimeAtStartOfDay());
            case CURRENT_MONTH:
                return new DateTime(LocalDate.now().withDayOfMonth(1).toDateTimeAtStartOfDay());
            default:
                return new DateTime(LocalDate.now().withDayOfYear(1).toDateTimeAtStartOfDay());
        }
    }

    /**
     * Gerenates a DataSet for a PieChart containing specific filters on work locations
     * after the given range.
     */
    public PieData generateWorkLocationDistribution(DataType type, Range range) {
        userData = Paper.book().read("user_data");

        // Current date
        DateTime curr = getStartOfRange(range);

        // Sum of time for all relevant locations
        float totalRelevantTime = userData.totalTimeWorked(type, curr);

        // All relevant locations
        ArrayList<RecordedLocation> relevantLocs = userData.getEventsInRange(type, curr);

        // List of entries for data
        List<PieEntry> entries = new ArrayList<>();

        // For each locaiton, get the total percentage of the distribution
        for (RecordedLocation r : relevantLocs) {
            float timeSpent = (1 - (totalRelevantTime / r.getTimeVisitedInRange(curr))) * 100;

            entries.add(new PieEntry(timeSpent, r.getTitle()));
        }

        PieDataSet set = new PieDataSet(entries, "Distribution of Work");
        return new PieData(set);
    }
}
