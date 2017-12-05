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

        import io.paperdb.Paper;

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

        // Initialize the chartCreator
        chartCreator = new PieChartCreator();

        // Initialize Paper
        Paper.init(this);

        DateTime startOfDay = DateTime.now().withTimeAtStartOfDay();

        RecordedLocation rl = new RecordedLocation(1,1, LocationType.WORK, true);
        rl.addInterval(new Interval(startOfDay, new DateTime(28800000 + startOfDay.getMillis())));

        RecordedLocation rl1 = new RecordedLocation(2,2, LocationType.WORK, true);
        rl1.addInterval(new Interval(startOfDay, new DateTime(36000000 + startOfDay.getMillis())));

        userData.storeNewLocation(rl);
        userData.storeNewLocation(rl1);

        Paper.book().write("user_data", userData);

        refreshData(PieChartCreator.DataType.TOTAL, PieChartCreator.Range.CURRENT_DAY);
    }

    /**
     * Refreshes the data of each PieChart in the display.
     */
    private void refreshData(PieChartCreator.DataType type,
                             PieChartCreator.Range range) {

        currentBalance.setData(chartCreator.generateGeneralDistribution(type, range));
        currentBalance.invalidate();

        idealBalance.setData(chartCreator.generateIdealDistribution(range));
    }
}
