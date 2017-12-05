package com.example.miniprince.workload;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import net.danlew.android.joda.JodaTimeAndroid;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import io.paperdb.Paper;

public class InitActivity extends AppCompatActivity {

        private final String TAG = "MA";
        private DiscreteSeekBar bar;
        private Button okButton;
        private Button read;
        private View.OnClickListener buttonListener;
        private UserData newUser;
        private Thread write;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_init);
            JodaTimeAndroid.init(this);

            initButtonListener();
            bar = findViewById(R.id.ratio_seek_bar);
            okButton = findViewById(R.id.ok_btn);
            okButton.setOnClickListener(buttonListener);
            read = findViewById(R.id.read_btn);
            read.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.i(TAG, "Attempting to read data.");
                    Log.i(TAG, Boolean.toString(write.isAlive()));

                    UserData data = Paper.book().read(getResources().getString(R.string.user_data));
                    Log.i(TAG, data.toString());
                }
            });


            Paper.init(this);
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
    }

