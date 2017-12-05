package com.example.miniprince.workload;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Layout for the OTHER tab of the LocationHistory Activity
 */

public class OtherTab extends android.support.v4.app.Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.other_tab_layout, container, false);
        return rootView;
    }
}
