package com.cz4013.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Created by melvynsng on 3/30/17.
 */
public class BookingSystemImpl implements BookingSystem {

    MonitorCallbackProxy mcp;
    private int objectID;
    public static int BSobjectID = 0;

    /**
     * Constructor that calls the initialise method.
     */
    public BookingSystemImpl() {
        initialise();
    }

    /**
     * Returns a String message output that the user on the client will see.
     * This method takes the facility name and the day string (in the format "0 1 2 3")
     *
     * @param facilityName
     * @param daysString
     * @return
     */
    public String getFacilityAvailability (String facilityName, String daysString) {
        if (isValidFacilityName(facilityName)) {
            return getInvaidFacilityNameErrorMessage(facilityName);
        }
        String result = facilityName + "\n";
        for (String dayStr : daysString.split(" ")) {
            int dayInt = Integer.parseInt(dayStr);
            result += getFacilityDayAvailability(facilityName, dayInt) + "\n";
        }
        return result;
    }

    /**
     * Returns a string for the bookings for the day specified.
     * @param facilityName
     * @param dayInt
     * @return
     */
    private String getFacilityDayAvailability (String facilityName, int dayInt) {
        Booking.DAYS day = getDay(dayInt);
        if (day == null) return "";
        Facility facility = getFacility(facilityName);
        ArrayList<Booking> bookings = facility.bookings.get(day);
        String result = getDayString(day) + " ";
        for (Booking b : bookings) {
            result += b.toString() + " ";
        }
        return result;
    }

    /**
     * Attempt to book a facility on a specified day, start and end time.
     * Returns the String message output for the attempt.
     * There is monitor callback executed here if the booking attempt is succeessful.
     * @param facilityName
     * @param dayInt
     * @param startTime
     * @param endTime
     * @return
     */
    public String bookFacility (String facilityName, int dayInt, int startTime, int endTime) {
        if (isValidFacilityName(facilityName)) {
            return getInvaidFacilityNameErrorMessage(facilityName);
        }
        Booking.DAYS day = getDay(dayInt);
        if (day == null) {
            return "Error Book Day " + Integer.toString(dayInt);
        }
        Facility facility = getFacility(facilityName);
        ArrayList<Booking> bookings = facility.bookings.get(day);
        Booking tempBooking = new Booking(startTime, endTime, day, facility);
        if (!tempBooking.isValid()) {
            return ("Error Book Time " + Integer.toString(startTime) + "-" + Integer.toString(endTime));
        }
        boolean isConflict = false;
        Booking conflictedBooking = null;
        for (Booking b : bookings) {
            if (tempBooking.conflict(b)) {
                isConflict = true;
                conflictedBooking = b;
            }
        }
        if (isConflict) {
            return getBookingConflictErrorMessage(tempBooking, conflictedBooking);
        }
        bookings.add(tempBooking);
        Confirmation confirmation = new Confirmation(tempBooking);
        int confirmationID = confirmation.save();
        mcp.displayAvailability(facilityName);
        return "Success Book ConfirmID " + Integer.toString(confirmationID);
    }

    /**
     * Returns the string message output for the user requesting a change in booking.
     * @param confirmID
     * @param offset
     * @return
     */
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
            return getBookingConflictErrorMessage(copy, conflictedBooking);
        }
        confirmation.booking = copy;
        String successMessage = "Change Request for ConfirmID " + Integer.toString(confirmIDInt) + " from " + initial.toString() + " to " + copy.toString() + " is successful!";
        bookings.remove(initial);
        bookings.add(copy);
        mcp.displayAvailability(copy.facility.name);
        return successMessage;
    }

    /**
     * Attempts to extend an existing booking by an offset (in minutes)
     * The booking is uniquely identified by the confirmation ID
     * Returns a String output message.
     * @param confirmID
     * @param offset
     * @return
     */
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
            getBookingConflictErrorMessage(copy, conflictedBooking);
        }
        confirmation.booking = copy;
        String successMessage = "Extend Request for ConfirmID " + Integer.toString(confirmIDInt) + " from " + initial.toString() + " to " + copy.toString() + " is successful!";
        bookings.remove(initial);
        bookings.add(copy);
        mcp.displayAvailability(copy.facility.name);
        return successMessage;
    };

    /**
     * Registers a clients to monitor a facility
     * @param facilityName
     * @param address
     * @param intervalMinutes
     * @param port
     * @return
     */
    public String monitorFacility (String facilityName, String address, int intervalMinutes, int port) {
        if (isValidFacilityName(facilityName)) {
            return getInvaidFacilityNameErrorMessage(facilityName);
        }
        Facility facility = getFacility(facilityName);
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

    /**
     * Returns a String message listing the names of the facilities in the system.
     * @return
     */
    public String listFacilities () {
        String result = "The list of facilities are:\n";
        for (Facility f : Facility.facilityHashMap.values()) {
            result += f.name + "\n";
        }
        return result;
    }


    /**
     * To initialise dummy data in the system for testing purposes
     */
    private void initialise () {
        Facility meetingRoom1 = new Facility("MeetingRoom1");
        meetingRoom1.save();
        Facility LectureTheatre1 = new Facility("LectureTheatre1");
        LectureTheatre1.save();
        BSobjectID += 1;
        this.objectID = BSobjectID;
    }

    /**
     * To map an integer to an enum in DAYS
     * @param dayInt
     * @return
     */
    private Booking.DAYS getDay (int dayInt) {
        Booking.DAYS day = null;
        switch (dayInt) {
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

    /**
     * To map the DAYS enum to a String. Returns an empty string for NULL input.
     * @param dayEnum
     * @return
     */
    private String getDayString (Booking.DAYS dayEnum) {
        switch (dayEnum) {
            case MON: return "MONDAY   ";
            case TUE: return "TUESDAY  ";
            case WED: return "WEDNESDAY";
            case THU: return "THURSDAY ";
            case FRI: return "FRIDAY   ";
            case SAT: return "SATURDAY ";
            case SUN: return "SUNDAY   ";
            default: return "";
        }
    }

    /**
     * To set the reference to monitor callback proxy object
     * @param mcp
     */
    public void setMonitorBroadcastProxy (MonitorCallbackProxy mcp) {
        this.mcp = mcp;
    }

    /**
     * Checks if the facility name is in the booking system
     * @param facilityName
     * @return
     */
    private boolean isValidFacilityName (String facilityName) {
        return Facility.facilityHashMap.containsKey(facilityName);
    }

    /**
     * Returns an error message string based on an invalid facility name
     * @param facilityName
     * @return
     */
    private String getInvaidFacilityNameErrorMessage (String facilityName) {
        return "Error: " + facilityName + " is not one of the facilities available.";
    }

    /**
     * Returns a Facility reference based on the facility name
     * @param facilityName
     * @return
     */
    private Facility getFacility (String facilityName) {
        return Facility.facilityHashMap.get(facilityName);
    }

    /**
     * Returns an error message string based on two conflicting booking objects.
     * @param booking1
     * @param booking2
     * @return
     */
    private String getBookingConflictErrorMessage (Booking booking1, Booking booking2) {
        return "Error: Booking Conflict between " + booking1.toString() + " and " + booking2.toString();
    }

}
