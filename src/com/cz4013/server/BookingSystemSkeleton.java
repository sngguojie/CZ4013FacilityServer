package com.cz4013.server;

import java.net.InetAddress;

/**
 * Created by melvynsng on 3/30/17.
 */
public class BookingSystemSkeleton implements RemoteObject {

    BookingSystem bs;

    public BookingSystemSkeleton () {
        this.bs = new BookingSystem();
    }

    public byte[] handleRequest (byte[] requestBody, InetAddress address, int port) {
        String requestString = new String(requestBody);
        String[] requestStringArray = requestString.split(" ");
        String methodName = requestStringArray[0];
        String facilityName;
        String result = "Error Unrecognised " + requestString;
        int day, start, end, confirmID, offset, interval;
        switch (methodName) {
            case "Get": // Idempotent
                facilityName = requestStringArray[1];
                day = Integer.parseInt(requestStringArray[2]);
                result = bs.getFacilityAvailability(facilityName, day);
                break;
            case "Book": // Non Idempotent
                facilityName = requestStringArray[1];
                day = Integer.parseInt(requestStringArray[2]);
                start = Integer.parseInt(requestStringArray[3]);
                end = Integer.parseInt(requestStringArray[4]);
                result = bs.bookFacility(facilityName, day, start, end);
                break;
            case "Change": // Non Idempotent
                confirmID = Integer.parseInt(requestStringArray[1]);
                offset = Integer.parseInt(requestStringArray[2]);
                result = bs.changeBooking(confirmID, offset);
                break;
            case "Monitor": // Non Idempotent
                facilityName = requestStringArray[1];
                interval = Integer.parseInt(requestStringArray[2]);
                result = bs.monitorFacility(facilityName, interval, address, port);
                break;
            case "List": // Idempotent
                result = bs.listFacilities();
                break;
            case "Create": // Idempotent
                facilityName = requestStringArray[1];
                result = bs.createFacility(facilityName);
                break;
            default: break;
        }
        return result.getBytes();
    }
}
