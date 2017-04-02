package com.cz4013.server;


public class Booking {

    public static enum DAYS {MON, TUE, WED, THU, FRI, SAT, SUN};
    public int start;
    public int end;
    public DAYS day;
    public Facility facility;

    public Booking (int start, int end, DAYS day, Facility facility) {
        this.start = start;
        this.end = end;
        this.day = day;
        this.facility = facility;
    }

    /**
     * Returns a String of the start time and end time of the booking instance in the format "12:30-13:30"
     * @return
     */
    public String toString () {
        int startHour = start / 60;
        int startMinute = start % 60;
        int endHour = end / 60;
        int endMinute = end % 60;
        return Integer.toString(startHour) + ":" + Integer.toString(startMinute) + "-"
                + Integer.toString(endHour) + ":" + Integer.toString(endMinute);
    }

    /**
     * Compares with another booking instance to check for timing conflict between the two bookings
     * @param other
     * @return
     */
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

    /**
     * Checks if the booking is valid based on the input data of the booking
     * @return
     */
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
