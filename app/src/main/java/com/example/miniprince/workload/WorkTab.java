package com.example.miniprince.workload;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Layout for the WORK history tab of the LocationHistory activity.
 */

public class WorkTab extends android.support.v4.app.Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.work_tab_layout, container, false);
        return rootView;
    }
}
