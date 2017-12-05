package com.example.miniprince.workload;

import com.github.mikephil.charting.charts.PieChart;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;

import io.paperdb.Paper;

/**
 * Generates PieCharts based on the current user data, and the specified type.
 */

public class PieChartCreator {
    // Data to draw from
    private UserData userData;

    public enum DataType {
        TOTAL, IDEAL, WORK_SAVED, WORK_UNSAVED,
    }

    public enum Range {
        CURRENT_DAY,
        CURRENT_WEEK,
        CURRENT_MONTH,
        CURRENT_YEAR
    }

    /**
     * Generates a PieChart with data of the given type in the given range.
     * @param type the type of data to be considered
     * @param range the range of time in which to consider data
     * @return a PieChart including data of the given type in the given range
     */
    public PieChart generate(DataType type, Range range) {
        userData = Paper.book().read("user_data");

        DateTime startThreshold = getStartOfRange(range);
    }

    /**
     * Generates the corresponding range to the given type.
     * @param range the range type
     * @return the relevant Interval
     */
    private DateTime getStartOfRange(Range range) {
        switch (range) {
            case CURRENT_DAY:
                return new DateTime(new LocalDate().now());
            case CURRENT_WEEK:
                return new DateTime(new LocalDate().now().weekOfWeekyear());
            case CURRENT_MONTH:
                return new DateTime(new LocalDate().now().monthOfYear());
            default:
                return new DateTime(new LocalDate().now().year());
        }
    }
}
