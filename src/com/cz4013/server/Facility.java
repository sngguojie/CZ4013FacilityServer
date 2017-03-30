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

    public boolean save () {
        if (facilityHashMap.containsKey(this.name)) {
            return false;
        }
        facilityHashMap.put(this.name, this);
        return true;
    }

    public String getWeekAvailability () {
        String result = this.name + "\n";
        for (Booking.DAYS d : this.bookings.keySet()) {
            String dayString = getDayString(d);
            result += dayString + " ";
            for (Booking b : this.bookings.get(d)) {
                result += b.toString() + " ";
            }
            result += "\n";
        }
        return result;
    }

    public String getDayString (Booking.DAYS Bday) {
        switch (Bday) {
            case MON: return "MON";
            case TUE: return "TUE";
            case WED: return "WED";
            case THU: return "THU";
            case FRI: return "FRI";
            case SAT: return "SAT";
            case SUN: return "SUN";
            default: return null;
        }
    }

}
