package com.cz4013.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Created by melvynsng on 3/30/17.
 */
public class BookingSystemImpl implements BookingSystem {

    MonitorBroadcastProxy mbp;
    private int objectID;
    public static int BSobjectID = 0;

    public BookingSystemImpl() {
        initialise();
    }

    public String getFacilityAvailability (String facilityName, String d) {
        if (!Facility.facilityHashMap.containsKey(facilityName)) {
            return "Error Get Unrecognised FacilityName " + facilityName;
        }
        String result = "";
        String[] daysArray = d.split(" ");
        for (String s : daysArray) {
            result += getFacilityDayAvailability(facilityName, Integer.parseInt(s));
        }
        return result;
    }
    private String getFacilityDayAvailability (String facilityName, int d) {
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
        mbp.displayAvailability(facilityName);
        return "Success Book ConfirmID " + Integer.toString(confirmationID);
    }

    public String changeBooking (String confirmID, int offset) {
        int confirmIDInt = Integer.parseInt(confirmID);
        if (!Confirmation.confirmationHashMap.containsKey(confirmIDInt)) {
            return "Error Change ConfirmID " + Integer.toString(confirmIDInt);
        }
        Confirmation confirmation = Confirmation.confirmationHashMap.get(confirmIDInt);
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
        String successMessage = "Change Request for ConfirmID " + Integer.toString(confirmIDInt) + " from " + initial.toString() + " to " + copy.toString() + " is successful!";
        bookings.remove(initial);
        bookings.add(copy);
        mbp.displayAvailability(copy.facility.name);
        return successMessage;
    }

    public String extendBooking (String confirmID, int offset){
        int confirmIDInt = Integer.parseInt(confirmID);
        if (!Confirmation.confirmationHashMap.containsKey(confirmIDInt)) {
            return "Error Extend ConfirmID " + Integer.toString(confirmIDInt);
        }
        Confirmation confirmation = Confirmation.confirmationHashMap.get(confirmIDInt);
        Booking initial = confirmation.booking;
        Booking copy = new Booking(initial.start, initial.end + offset, initial.day, initial.facility);
        if (!copy.isValid()) {
            return "Error Extend Offset " + Integer.toString(offset);
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
            return "Error Extend Conflict " + copy.toString() + " " + conflictedBooking.toString();
        }
        confirmation.booking = copy;
        String successMessage = "Extend Request for ConfirmID " + Integer.toString(confirmIDInt) + " from " + initial.toString() + " to " + copy.toString() + " is successful!";
        bookings.remove(initial);
        bookings.add(copy);
        mbp.displayAvailability(copy.facility.name);
        return successMessage;
    };

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
        String result = "The list of facilities are:\n";
        for (Facility f : Facility.facilityHashMap.values()) {
            result += f.name + "\n";
        }
        return result;
    }



    private void initialise () {
        Facility meetingRoom1 = new Facility("MeetingRoom1");
        meetingRoom1.save();
        Facility LectureTheatre1 = new Facility("LectureTheatre1");
        LectureTheatre1.save();
        BSobjectID += 1;
        this.objectID = BSobjectID;
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

    public void setMonitorBroadcastProxy (MonitorBroadcastProxy mbp) {
        this.mbp = mbp;
    }

    public int getObjectID(){
        return this.objectID;
    }

}
