package com.example.miniprince.workload;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;

import java.util.ArrayList;
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
    public PieData generateGeneralDistribution(DataType type, Range range) {
        userData = Paper.book().read("user_data");

        DateTime startThreshold = getStartOfRange(range);
        float timeSpent = userData.totalTimeWorked(type, startThreshold);

        float timeInRange = range.getVal();

        if (timeSpent != 0) {
            timeSpent = (1 - (timeInRange / timeSpent)) * 100;
        }

        float otherPercentage = 100 - timeSpent;

        // Create entry for total time worked
        PieEntry hoursWorked = new PieEntry(timeSpent, "Work");

        // Create entry for the rest of the time
        PieEntry hoursOther = new PieEntry(otherPercentage, "Other");

        List<PieEntry> entries = new ArrayList<>();
        entries.add(hoursWorked);
        entries.add(hoursOther);

        PieDataSet set = new PieDataSet(entries, "Current Balance");
        return new PieData(set);
    }

    /**
     * Generates data for the user's ideal distribution in the given range.
     * @param range
     * @return
     */
    public PieData generateIdealDistribution(Range range) {
        float timeSpent = userData.getIdealTimeInRange(range);

        float timeInRange = range.getVal();

        if (timeSpent != 0) {
            timeSpent = (1 - (timeInRange / timeSpent)) * 100;
        }

        float otherPercentage = 100 - timeSpent;

        // Create entry for total time worked
        PieEntry hoursWorked = new PieEntry(timeSpent, "Work");

        // Create entry for the rest of the time
        PieEntry hoursOther = new PieEntry(otherPercentage, "Other");

        List<PieEntry> entries = new ArrayList<>();
        entries.add(hoursWorked);
        entries.add(hoursOther);

        PieDataSet set = new PieDataSet(entries, "Current Balance");
        return new PieData(set);
    }

    /**
     * Generates the corresponding range to the given type.
     * @param range the range type
     * @return the relevant Interval
     */
    private DateTime getStartOfRange(Range range) {
        switch (range) {
            case CURRENT_DAY:
                return new DateTime(new LocalDate().now().toDateTimeAtStartOfDay());
            case CURRENT_WEEK:
                return new DateTime(new LocalDate().now().weekOfWeekyear());
            case CURRENT_MONTH:
                return new DateTime(new LocalDate().now().monthOfYear());
            default:
                return new DateTime(new LocalDate().now().year());
        }
    }
}
