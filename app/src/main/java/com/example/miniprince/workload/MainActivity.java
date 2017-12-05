package com.example.miniprince.workload;

        import android.app.Fragment;
        import android.app.FragmentManager;
        import android.content.Context;
        import android.content.Intent;
        import android.os.Build;
        import android.support.annotation.RequiresApi;
        import android.support.v4.widget.DrawerLayout;
        import android.support.v7.app.ActionBarDrawerToggle;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.util.Log;
        import android.view.LayoutInflater;
        import android.view.Menu;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.AdapterView;
        import android.widget.ArrayAdapter;
        import android.widget.BaseAdapter;
        import android.widget.ImageView;
        import android.widget.ListView;
        import android.widget.RelativeLayout;
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

/**
 * DRAWER LAYOUT CODE FROM: http://codetheory.in/android-navigation-drawer/
 */
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

    // The view for the nav drawer
    ListView mDrawerList;

    // The layout for the pane of the nav drawer
    RelativeLayout mDrawerPane;

    // The toggler for the nav pane
    private ActionBarDrawerToggle mDrawerToggle;

    // This drawer layout
    private DrawerLayout mDrawerLayout;

    // Items in the nav pane
    ArrayList<NavItem> mNavItems = new ArrayList<NavItem>();

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        JodaTimeAndroid.init(this);

        mNavItems.add(new NavItem("Balance", "See your current balance.", R.drawable.temp_nav));
        mNavItems.add(new NavItem("Location", "Edit what's being recorded right now.", R.drawable.temp_nav));

        // Initialize the drawer layout
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        // Populate the drawer
        mDrawerPane = (RelativeLayout) findViewById(R.id.drawerPane);
        mDrawerList = (ListView) findViewById(R.id.navList);
        DrawerListAdapter adapter = new DrawerListAdapter(this, mNavItems);
        mDrawerList.setAdapter(adapter);

        // Click listeners for nav drawer
        // Drawer Item click listeners
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItemFromDrawer(position);
            }
        });

        initDrawerToggle();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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

        RecordedLocation rl = new RecordedLocation(1, 1, LocationType.WORK, true, "Home");
        rl.addInterval(new Interval(startOfDay, new DateTime(28800000 + startOfDay.getMillis())));

        RecordedLocation rl1 = new RecordedLocation(2, 2, LocationType.WORK, true, "Snell");
        rl1.addInterval(new Interval(startOfDay, new DateTime(36000000 + startOfDay.getMillis())));

        userData.storeNewLocation(rl);
        userData.storeNewLocation(rl1);

        Paper.book().write("user_data", userData);

        currRange = PieChartCreator.Range.CURRENT_DAY;

        initSpinner();
        refreshData();
    }

    /**
     * Initializes the toggle for the nav drawer
     */
    private void initDrawerToggle() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                Log.d(TAG, "onDrawerClosed: " + getTitle());

                invalidateOptionsMenu();
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }


    /**
     * Refreshes the data of each PieChart in the display.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void refreshData() {
        PieDataSet current = chartCreator.generateGeneralDistribution(PieChartCreator.DataType.TOTAL, currRange);

        current.setColors(new ArrayList<Integer>(Arrays.asList(getColor(R.color.colorAccent), getColor(R.color.colorPrimaryDarkUpshade))));
        current.setValueLineWidth(10f);
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
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i) {
                    case 0:
                        currRange = PieChartCreator.Range.CURRENT_DAY;
                        refreshData();
                        break;
                    case 1:
                        currRange = PieChartCreator.Range.CURRENT_WEEK;
                        refreshData();
                        break;
                    case 2:
                        currRange = PieChartCreator.Range.CURRENT_MONTH;
                        refreshData();
                        break;
                    case 3:
                        currRange = PieChartCreator.Range.CURRENT_YEAR;
                        refreshData();
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        };

        balanceSpinner.setOnItemSelectedListener(listener);
    }

    /*
* Called when a particular item from the navigation drawer
* is selected.
* */
    private void selectItemFromDrawer(int position) {
        switch (position) {
            case 0:
                // Close the drawer
                mDrawerLayout.closeDrawer(mDrawerPane);
               // startActivity(new Intent(MainActivity.this, MainActivity.class));
                break;
            case 1:
                // Close the drawer
                mDrawerLayout.closeDrawer(mDrawerPane);
                startActivity(new Intent(MainActivity.this, CurrentArea.class));
                break;
        }
    }

    class NavItem {
        String mTitle;
        String mSubtitle;
        int mIcon;

        public NavItem(String title, String subtitle, int icon) {
            mTitle = title;
            mSubtitle = subtitle;
            mIcon = icon;
        }
    }

    class DrawerListAdapter extends BaseAdapter {

        Context mContext;
        ArrayList<NavItem> mNavItems;

        public DrawerListAdapter(Context context, ArrayList<NavItem> navItems) {
            mContext = context;
            mNavItems = navItems;
        }

        @Override
        public int getCount() {
            return mNavItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mNavItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.drawer_item, null);
            } else {
                view = convertView;
            }

            TextView titleView = (TextView) view.findViewById(R.id.title);
            TextView subtitleView = (TextView) view.findViewById(R.id.subTitle);
            ImageView iconView = (ImageView) view.findViewById(R.id.icon);

            titleView.setText(mNavItems.get(position).mTitle);
            subtitleView.setText(mNavItems.get(position).mSubtitle);
            iconView.setImageResource(mNavItems.get(position).mIcon);

            return view;
        }
    }
}