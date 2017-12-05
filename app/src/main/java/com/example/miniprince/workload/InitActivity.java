package com.example.miniprince.workload;

import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import net.danlew.android.joda.JodaTimeAndroid;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.paperdb.Paper;

public class InitActivity extends AppCompatActivity {

    private final String TAG = "MA";
    private DiscreteSeekBar bar;
    private Button okButton;

    private View.OnClickListener buttonListener;
    private UserData newUser;
    private Thread write;

    private TextView infoView;
    private PieChart idealChart;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);
        JodaTimeAndroid.init(this);

        initButtonListener();
        bar = findViewById(R.id.ratio_seek_bar);
        okButton = findViewById(R.id.ok_btn);
        okButton.setOnClickListener(buttonListener);

        updateChart();

        bar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (bar.onTouchEvent(motionEvent)) {
                    Log.i(TAG, "Change");
                    updateChart();
                    updateText();
                    view.performClick();
                }
                return true;
            }
        });

        infoView = findViewById(R.id.init_info_text);

        Paper.init(this);
    }

    /**
     * Initializes the PieChart
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void updateChart() {
        this.idealChart = findViewById(R.id.init_pie_chart);

        int progress = (int) ((((float) bar.getProgress()) / 168f) * 100f);

        Log.i(TAG, " " + progress);

        PieEntry hours = new PieEntry(progress, "Work");
        PieEntry other = new PieEntry(100 - progress, "Other");

        List<PieEntry> entries = new ArrayList<>();
        entries.add(hours);
        entries.add(other);

        PieDataSet set = new PieDataSet(entries, "");
        set.setValueLineColor(getColor(R.color.colorAccent));
        set.setValueLineWidth(10f);
        set.setColors(new ArrayList<Integer>(Arrays.asList(getColor(R.color.colorAccent), getColor(R.color.colorPrimaryDarkUpshade))));
        idealChart.setData(new PieData(set));
        idealChart.setBackgroundColor(getColor(R.color.colorPrimaryDark));
        idealChart.setHoleColor(getColor(R.color.colorPrimaryDark));
        idealChart.setTransparentCircleAlpha(0);
        idealChart.setHoleRadius(50f);
        idealChart.setUsePercentValues(true);
        idealChart.setHighlightPerTapEnabled(false);

        idealChart.invalidate();
    }

    /**
     * Initializes the OnClickListener for the okay button.
     */
    private void initButtonListener() {
        Log.i(TAG, "Initializing button listener.");
        this.buttonListener = new View.OnClickListener() {

            // Initializes the UserData, sets it to the selected value, and starts the next activity
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Ok button clicked.");

                long ideal = bar.getProgress() * 3600000;
                newUser = new UserData(ideal);

                write = new Thread(new Runnable() {
                    volatile boolean isRunning = true;

                    @Override
                    public void run() {
                        Log.i(TAG, "Starting to write data.");
                        Paper.book().write(getResources().getString(R.string.user_data), newUser);

                        while (isRunning) {
                            UserData temp = Paper.book().read(getResources().getString(R.string.user_data));
                            if (temp.getIdealTimePerWeek() == -1) {
                                Log.i(TAG, "Data being written.");
                            } else {
                                Log.i(TAG, "Data written.");
                                isRunning = false;
                            }
                        }

                        Log.i(TAG, "Thread ending.");
                        return;
                    }
                });
                write.start();

                startActivity(new Intent(InitActivity.this, MainActivity.class));
            }
        };
    }

    private void updateText() {
        int progress = (int) ((((float) bar.getProgress()) / 168f) * 100f);

        infoView.setText(bar.getProgress() + " hours is about " + progress + "% of the week.");
    }
}

