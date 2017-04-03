package com.cz4013.server;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class Facility {

    public static HashMap<String, Facility> facilityHashMap = new HashMap<String, Facility>();

    public String name;

    public HashMap<Booking.DAYS, ArrayList<Booking>> bookings = new HashMap<Booking.DAYS, ArrayList<Booking>>();
    public ArrayList<Monitor> monitorList = new ArrayList<Monitor>();


    public Facility (String name) {
        this.name = name;
        for (Booking.DAYS day : Booking.DAYS.values()) {
            this.bookings.put(day, new ArrayList<Booking>());
        }
    }

    /**
     * To store the facility with a unique ID
     * @return
     */
    public boolean save () {
        if (facilityHashMap.containsKey(this.name)) {
            return false;
        }
        facilityHashMap.put(this.name, this);
        return true;
    }

    /**
     * To get the availability of the whole week for the facility instance
     * @return
     */
    public String getWeekAvailability () {
        String result = this.name + "\n";

        for (Booking.DAYS dayEnum : Booking.DAYS.values()) {
            String dayString = getDayString(dayEnum);
            result += dayString + " ";
            for (Booking b : this.bookings.get(dayEnum)) {
                result += b.toString() + " ";
            }
            result += "\n";
        }
        return result;
    }

    /**
     * To get the String Representation of the day
     * @param dayEnum
     * @return
     */
    public String getDayString (Booking.DAYS dayEnum) {
        switch (dayEnum) {
            case MON: return "MONDAY   ";
            case TUE: return "TUESDAY  ";
            case WED: return "WEDNESDAY";
            case THU: return "THURSDAY ";
            case FRI: return "FRIDAY   ";
            case SAT: return "SATURDAY ";
            case SUN: return "SUNDAY   ";
            default: return null;
        }
    }

}
