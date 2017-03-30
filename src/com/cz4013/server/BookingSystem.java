package com.cz4013.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Created by melvynsng on 3/30/17.
 */
public class BookingSystem implements BookingSystemInterface {

    public BookingSystem () {
        initialise();
    }

    public String getFacilityAvailability (String facilityName, int d) {
        if (!Facility.facilityHashMap.containsKey(facilityName)) {
            return "Error Get Unrecognised FacilityName " + facilityName;
        }
        Booking.DAYS day = getDay(d);
        if (day == null) {
            return "Error Get Unrecognised Day " + Integer.toString(d);
        }
        Facility facility = Facility.facilityHashMap.get(facilityName);
        ArrayList<Booking> bookings = facility.bookings.get(day);
        String result = "Success Get " + facilityName + " " + getDayString(day) + " ";
        for (Booking b : bookings) {
            result += b.toString() + " ";
        }
        return result;
    }

    public String bookFacility (String facilityName, int d, int s, int e) {
        if (!Facility.facilityHashMap.containsKey(facilityName)) {
            return "Error Book FacilityName " + facilityName;
        }
        Booking.DAYS day = getDay(d);
        if (day == null) {
            return "Error Book Day " + Integer.toString(d);
        }
        Facility facility = Facility.facilityHashMap.get(facilityName);
        ArrayList<Booking> bookings = facility.bookings.get(day);
        Booking temp = new Booking(s, e, day, facility);
        if (!temp.isValid()) {
            return ("Error Book Time " + Integer.toString(s) + "-" + Integer.toString(e));
        }
        boolean isConflict = false;
        Booking conflictedBooking = null;
        for (Booking b : bookings) {
            if (temp.conflict(b)) {
                isConflict = true;
                conflictedBooking = b;
            }
        }
        if (isConflict) {
            return "Error Book Conflict " + temp.toString() + " " + conflictedBooking.toString();
        }
        bookings.add(temp);
        Confirmation confirmation = new Confirmation(temp);
        int confirmationID = confirmation.save();
        return "Success Book ConfirmID " + Integer.toString(confirmationID);
    }

    public String changeBooking (int confirmID, int offset) {
        if (!Confirmation.confirmationHashMap.containsKey(confirmID)) {
            return "Error Change ConfirmID " + Integer.toString(confirmID);
        }
        Confirmation confirmation = Confirmation.confirmationHashMap.get(confirmID);
        Booking initial = confirmation.booking;
        Booking copy = new Booking(initial.start + offset, initial.end + offset, initial.day, initial.facility);
        if (!copy.isValid()) {
            return "Error Change Offset " + Integer.toString(offset);
        }
        ArrayList<Booking> bookings = initial.facility.bookings.get(initial.day);
        boolean isConflict = false;
        Booking conflictedBooking = null;
        for (Booking b : bookings) {
            if (!b.equals(initial)) {
                if (copy.conflict(b)) {
                    isConflict = true;
                    conflictedBooking = b;
                }
            }
        }
        if (isConflict) {
            return "Error Change Conflict " + copy.toString() + " " + conflictedBooking.toString();
        }
        confirmation.booking = copy;
        bookings.remove(initial);
        bookings.add(copy);
        return "Success Change ConfirmID " + Integer.toString(confirmID) + " " + copy.toString();
    }

    public String monitorFacility (String facilityName, String address, int intervalMinutes, int port) {
        if (!Facility.facilityHashMap.containsKey(facilityName)) {
            return "Error Monitor FacilityName " + facilityName;
        }
        Facility facility = Facility.facilityHashMap.get(facilityName);
        long now = System.currentTimeMillis();
        long expiry = now + ((long) intervalMinutes) * 60000l;
        try {
            InetAddress inetAddress = InetAddress.getByName(address);
            Monitor monitor = new Monitor(inetAddress, port, expiry);
            facility.monitorList.add(monitor);
            return "Success Monitor";
        } catch (UnknownHostException uhe) {
            return "Error Unknown Host " + address;
        }
    }

    public String listFacilities () {
        String result = "Success List";
        for (Facility f : Facility.facilityHashMap.values()) {
            result += " " + f.name;
            System.out.println(f.name);
        }
        return result;
    }

    public String createFacility (String facilityName) {
        Facility newFacility = new Facility(facilityName);
        newFacility.save();
        return "Success Created facility " + facilityName;
    }

    public String getFacilityWeekAvailability (String facilityName) {
        if (!Facility.facilityHashMap.containsKey(facilityName)) {
            return "Error Unrecognised FacilityName " + facilityName;
        }
        Facility facility = Facility.facilityHashMap.get(facilityName);
        String result = facility.name + "\n";
        for (Booking.DAYS d : facility.bookings.keySet()) {
            String dayString = getDayString(d);
            result += dayString + " ";
            for (Booking b : facility.bookings.get(d)) {
                result += b.toString() + " ";
            }
            result += "\n";
        }
        return result;
    }

    private void initialise () {
        Facility meetingRoom1 = new Facility("MeetingRoom1");
        meetingRoom1.save();
        Facility LectureTheatre1 = new Facility("LectureTheatre1");
        LectureTheatre1.save();
    }

    private Booking.DAYS getDay (int d) {
        Booking.DAYS day = null;
        switch (d) {
            case 0: day = Booking.DAYS.MON; break;
            case 1: day = Booking.DAYS.TUE; break;
            case 2: day = Booking.DAYS.WED; break;
            case 3: day = Booking.DAYS.THU; break;
            case 4: day = Booking.DAYS.FRI; break;
            case 5: day = Booking.DAYS.SAT; break;
            case 6: day = Booking.DAYS.SUN; break;
            default: break;
        }
        return day;
    }

    private String getDayString (Booking.DAYS day) {
        switch (day) {
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
