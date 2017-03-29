package com.cz4013.server;

import java.net.InetAddress;

public interface BookingSystemInterface {
    public String getFacilityAvailability (String facilityName, int d);

    public String bookFacility (String facilityName, int d, int s, int e);

    public String changeBooking (int confirmID, int offset);

    public String monitorFacility (String facilityName, int intervalMinutes, InetAddress address, int port);

    public String listFacilities ();

    public String createFacility (String facilityName);

}
