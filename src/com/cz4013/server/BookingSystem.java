package com.cz4013.server;

import java.net.InetAddress;
import java.net.UnknownHostException;

public interface BookingSystem {
    /**
     * Returns the string of the facility availability given the facility name and the day string in the form "1 2 3"
     * Returns the appropriate error messages input provided is incorrect
     * @param facilityName
     * @param daysString
     * @return
     */
    public String getFacilityAvailability (String facilityName, String daysString);

    /**
     * Returns the string message output for the attempt to book a facility at a given start and end time
     * Returns the confirmation ID of the booking if the attempt is successful.
     * Returns the appropriate error messages input provided is incorrect
     * @param facilityName
     * @param dayInt
     * @param startTime
     * @param endTime
     * @return
     */
    public String bookFacility (String facilityName, int dayInt, int startTime, int endTime);

    /**
     * Returns the string message output for the attempt to change the time of an existing booking by an offset
     * The confirmation ID of the booking is used to uniquely identify the booking
     * Returns the appropriate error messages input provided is incorrect
     * @param confirmID
     * @param offset
     * @return
     */
    public String changeBooking (String confirmID, int offset);

    /**
     * Returns the string message output for the attempt to extend the end time of an existing booking by an offset
     * The confirmation ID of the booking is used to uniquely identify the booking
     * Returns the appropriate error messages input provided is incorrect
     * @param confirmID
     * @param offset
     * @return
     */
    public String extendBooking (String confirmID, int offset);

    /**
     * Returns a string message output to indicate whether the request to register to monitor a facility is succesful.
     * The monitor callback is set up based on the address and port provided as arguments.
     * The monitor will last for a specified number of minutes indicated by the interval minutes
     * @param facilityName
     * @param address
     * @param intervalMinutes
     * @param port
     * @return
     */
    public String monitorFacility (String facilityName, String address, int intervalMinutes, int port);

    /**
     * Returns a string output of the list of facilities in the booking system
     * @return
     */
    public String listFacilities ();
}
