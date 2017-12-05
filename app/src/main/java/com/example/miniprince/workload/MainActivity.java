package com.example.miniprince.workload;

        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.util.Log;

        import com.github.mikephil.charting.charts.PieChart;
        import com.github.mikephil.charting.data.PieData;
        import com.github.mikephil.charting.data.PieDataSet;
        import com.github.mikephil.charting.data.PieEntry;

        import net.danlew.android.joda.JodaTimeAndroid;

        import org.joda.time.DateTime;
        import org.joda.time.Interval;

        import java.util.ArrayList;
        import java.util.Calendar;
        import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MA";

    // The currently selected distribution
    private PieChart currentBalance;

    // The ideal distribution
    private PieChart idealBalance;

    // The user data to read
    private UserData userData;

    // The creator for the charts
    private PieChartCreator chartCreator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        JodaTimeAndroid.init(this);

        // Initialize the chart for current balance
        currentBalance = (PieChart) findViewById(R.id.current_balance);

        // Initialize the chart for the ideal balance
        idealBalance = (PieChart) findViewById(R.id.ideal_balance);

        // Read the current user data
        userData = new UserData(144000000);

        DateTime startOfDay = DateTime.now().withTimeAtStartOfDay();

        RecordedLocation rl = new RecordedLocation(1,1, LocationType.WORK, true);
        rl.addInterval(new Interval(startOfDay, new DateTime(28800000 + startOfDay.getMillis())));

        RecordedLocation rl1 = new RecordedLocation(2,2, LocationType.WORK, true);
        rl1.addInterval(new Interval(startOfDay, new DateTime(36000000 + startOfDay.getMillis())));

     //   userData.storeNewLocation(rl);
     //   userData.storeNewLocation(rl1);

        refreshData();
    }

    /**
     * Refreshes the data of each PieChart in the display.
     */
    private void refreshData(PieChartCreator.DataType type,
                             PieChartCreator.Range range) {
        Log.i(TAG, Long.toString(userData.getTimeWorkedThisDay()));

        currentBalance = chartCreator.generate(type, range);

        idealBalance = chartCreator.generate(PieChartCreator.DataType.IDEAL, range);

        float timeInDay = 86400000;

        float dayPercentage = (1 - (timeInDay / userData.getTimeWorkedThisDay())) * 100;
        Log.i(TAG, Float.toString(dayPercentage));
        float otherPercentage = 100 - dayPercentage;

        // Create entry for total time worked
        PieEntry hoursWorked = new PieEntry(dayPercentage, "Work");

        // Create entry for the rest of the time
        PieEntry hoursOther = new PieEntry(otherPercentage, "Other");

        List<PieEntry> entries = new ArrayList<>();
        entries.add(hoursWorked);
        entries.add(hoursOther);

        PieDataSet set = new PieDataSet(entries, "Current Balance");
        PieData data = new PieData(set);

        currentBalance.setData(data);
        currentBalance.invalidate();
    }
}
