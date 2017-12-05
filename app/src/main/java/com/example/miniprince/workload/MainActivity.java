package com.example.miniprince.workload;

        import android.os.Build;
        import android.support.annotation.RequiresApi;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.util.Log;
        import android.view.View;
        import android.widget.AdapterView;
        import android.widget.ArrayAdapter;
        import android.widget.Spinner;
        import android.widget.TextView;

        import com.github.mikephil.charting.charts.PieChart;
        import com.github.mikephil.charting.data.PieData;
        import com.github.mikephil.charting.data.PieDataSet;
        import com.github.mikephil.charting.data.PieEntry;

        import net.danlew.android.joda.JodaTimeAndroid;

        import org.joda.time.DateTime;
        import org.joda.time.Interval;

        import java.util.ArrayList;
        import java.util.Arrays;
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

    // The current range of data being displayed
    private PieChartCreator.Range currRange;

    // the spinner menu for balance
    private Spinner balanceSpinner;

    @RequiresApi(api = Build.VERSION_CODES.M)
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

        RecordedLocation rl = new RecordedLocation(1,1, LocationType.WORK, true, "Home");
        rl.addInterval(new Interval(startOfDay, new DateTime(28800000 + startOfDay.getMillis())));

        RecordedLocation rl1 = new RecordedLocation(2,2, LocationType.WORK, true, "Snell");
        rl1.addInterval(new Interval(startOfDay, new DateTime(36000000 + startOfDay.getMillis())));

        userData.storeNewLocation(rl);
        userData.storeNewLocation(rl1);

        Paper.book().write("user_data", userData);

        currRange = PieChartCreator.Range.CURRENT_DAY;

        initSpinner();
        refreshData();
    }

    /**
     * Refreshes the data of each PieChart in the display.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void refreshData() {
        PieDataSet current = chartCreator.generateGeneralDistribution(PieChartCreator.DataType.TOTAL, currRange);

        current.setColors(new ArrayList<Integer>(Arrays.asList(getColor(R.color.colorAccent), getColor(R.color.colorPrimaryDarkUpshade))));

        currentBalance.setBackgroundColor(getColor(R.color.colorPrimary));
        currentBalance.setHoleColor(getColor(R.color.colorPrimary));
        currentBalance.setTransparentCircleAlpha(0);
        currentBalance.setHoleRadius(50f);
        currentBalance.setUsePercentValues(true);
        currentBalance.setHighlightPerTapEnabled(false);
        currentBalance.setData(new PieData(current));
        currentBalance.invalidate();

        PieDataSet ideal = chartCreator.generateIdealDistribution(currRange);
        ideal.setColors(new ArrayList<Integer>(Arrays.asList(getColor(R.color.colorAccent), getColor(R.color.colorPrimaryDarkUpshade))));

        idealBalance.setBackgroundColor(getColor(R.color.colorPrimary));
        idealBalance.setHoleColor(getColor(R.color.colorPrimary));
        idealBalance.setTransparentCircleAlpha(0);
        idealBalance.setHoleRadius(50f);
        idealBalance.setUsePercentValues(true);
        idealBalance.setHighlightPerTapEnabled(false);
        idealBalance.setData(new PieData(current));
        idealBalance.invalidate();

        idealBalance.setData(new PieData(ideal));
    }

    /**
     * Initializes the spinner for the balance
     */
    private void initSpinner() {
        balanceSpinner = (Spinner) findViewById(R.id.balance_dist_spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.ranges_array, android.R.layout.simple_spinner_item);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        balanceSpinner.setAdapter(adapter);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        };

        balanceSpinner.setOnItemSelectedListener(listener);
    }
}
