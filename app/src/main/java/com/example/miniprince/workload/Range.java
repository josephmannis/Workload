package com.example.miniprince.workload;

import java.io.Serializable;

/**
 * Represents a range of time.
 */
public class Range implements Serializable {
    // The time this range began, in ms
    private long start;

    // The time this range ended, in ms
    private long end;

    public Range(long start) {
        this.start = start;
    }

    /**
     * Gets the length of this range from start to end, in ms.
     * @return the range of length of this range from start to end.
     */
    public long getLength() {
        if (end < start) {
            throw new IllegalArgumentException("Invalid range.");
        }

        return end - start;
    }

    /**
     * Determines if the given time is within this range
     * @param mil the time to check, in ms
     * @return whether or not the given time is within range
     */
    public boolean isWithin(long mil) {
        return mil >= start && mil <= end;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }
}
