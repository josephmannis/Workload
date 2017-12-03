package com.example.miniprince.workload;

import android.support.annotation.IntDef;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Typing for different forms of SavedLocations.
 */

public class LocationType implements Serializable {
    public static final int WORK = 0;
    public static final int OTHER = 1;

    @IntDef({WORK, OTHER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Location {}
}
