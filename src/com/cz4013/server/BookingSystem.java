package com.cz4013.server;

import java.net.InetAddress;
import java.net.UnknownHostException;

public interface BookingSystem {
    public String getFacilityAvailability (String facilityName, int d);

    public String bookFacility (String facilityName, int d, int s, int e);

    public String changeBooking (int confirmID, int offset);

    public String monitorFacility (String facilityName, String address, int intervalMinutes, int port);

    public String listFacilities ();
}
