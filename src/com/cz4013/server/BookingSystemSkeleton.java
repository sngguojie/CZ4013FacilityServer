package com.cz4013.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by melvynsng on 3/30/17.
 */
public class BookingSystemSkeleton implements BookingSystem, RemoteObject {

    BookingSystemImpl bs;

    private final int MAX_BYTE_SIZE = 1024;
    private final int BYTE_CHUNK_SIZE = 4;
    private enum DATATYPE{STRING, INTEGER};
    CommunicationModule communicationModule;

    public BookingSystemSkeleton () {

    }

    public byte[] handleRequest (byte[] requestBody) {
        System.out.println(new String(requestBody));
        Data unmarshalledData = MarshalModule.unmarshal(requestBody);
        System.out.println(unmarshalledData.toString());
        String methodName = unmarshalledData.getMethodId();
        System.out.println(methodName);

        String result = "Error Unrecognised";
        String facilityName, address;
        int day, start, end, confirmID, offset, interval, port;
        switch (methodName) {
            case "getFacilityAvailability": // Idempotent
                facilityName = unmarshalledData.getStringList().get(1);
                day = Integer.parseInt(unmarshalledData.getStringList().get(2));
                result = getFacilityAvailability(facilityName, day);
                break;
            case "bookFacility": // Non Idempotent
                facilityName = unmarshalledData.getStringList().get(1);
                day = Integer.parseInt(unmarshalledData.getStringList().get(2));
                start = Integer.parseInt(unmarshalledData.getStringList().get(3));
                end = Integer.parseInt(unmarshalledData.getStringList().get(4));
                result = bookFacility(facilityName, day, start, end);
                break;
            case "changeBooking": // Non Idempotent
                confirmID = Integer.parseInt(unmarshalledData.getStringList().get(1));
                offset = Integer.parseInt(unmarshalledData.getStringList().get(2));
                result = changeBooking(confirmID, offset);
                break;
            case "extendBooking": // Non Idempotent
                confirmID = Integer.parseInt(unmarshalledData.getStringList().get(1));
                offset = Integer.parseInt(unmarshalledData.getStringList().get(2));
                result = extendBooking(confirmID, offset);
                break;
            case "monitorFacility": // Non Idempotent
                facilityName = unmarshalledData.getStringList().get(1);
                address = unmarshalledData.getStringList().get(2);
                interval = Integer.parseInt(unmarshalledData.getStringList().get(3));
                port = Integer.parseInt(unmarshalledData.getStringList().get(4));
                result = monitorFacility(facilityName, address, interval, port);
                break;
            case "listFacilities": // Idempotent
                result = listFacilities();
                break;
            default: break;
        }
        String[] stringArray = {"BookingSystemProxy", methodName, result};
        int[] intArray = {1};
        return MarshalModule.marshal(stringArray, intArray);
    }

    public String getFacilityAvailability (String facilityName, int d){
        return bs.getFacilityAvailability(facilityName, d);
    };

    public String bookFacility (String facilityName, int d, int s, int e){
        return bs.bookFacility(facilityName, d, s, e);
    };

    public String changeBooking (int confirmID, int offset){
        return bs.changeBooking ( confirmID,  offset);
    };

    public String extendBooking (int confirmID, int offset){
        return bs.extendBooking ( confirmID,  offset);
    };

    public String monitorFacility (String facilityName, String address, int intervalMinutes, int port){
        return bs.monitorFacility ( facilityName,  address,  intervalMinutes,  port);
    };

    public String listFacilities (){
        return bs.listFacilities ();
    };

    public void setCommunicationModule (CommunicationModule cm) {
        this.communicationModule = cm;
    }
    public void setBookingSystem (BookingSystemImpl bsi) {
        this.bs = bsi;
    }
}