package com.cz4013.server;

/**
 * Created by melvynsng on 3/29/17.
 */
public class Booking {

    public int start;
    public int end;
    public Facility.DAYS day;
    public Facility facility;

    public Booking (int start, int end, Facility.DAYS day, Facility facility) {
        this.start = start;
        this.end = end;
        this.day = day;
        this.facility = facility;
    }

    public String toString () {
        return Integer.toString(start) + "-"
                + Integer.toString(end);
    }

    public boolean conflict (Booking other) {
        if (!this.facility.equals(other.facility)) {
            return false;
        }
        if (this.start > other.end) {
            return false;
        } else if (other.start > this.end) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isValid () {
        if (this.start < 0) {
            return false;
        }
        if (this.end > 24 * 60) {
            return false;
        }
        if (this.end <= this.start) {
            return false;
        }
        return true;
    }

}
