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
        Data unmarshalledData = MarshalModule.unmarshal(requestBody);
        String methodName = unmarshalledData.getMethodId();

        String result = "Error Unrecognised Method " + methodName;
        String facilityName, address, confirmID, days;
        int day, start, end, offset, interval, port;
        switch (methodName) {
            case "getFacilityAvailability": // Idempotent
                facilityName = unmarshalledData.getStringList().get(0);
                days = unmarshalledData.getStringList().get(1);
                result = getFacilityAvailability(facilityName, days);
                break;
            case "bookFacility": // Non Idempotent
                facilityName = unmarshalledData.getStringList().get(0);
                day = unmarshalledData.getIntList().get(0);
                start = unmarshalledData.getIntList().get(1);
                end = unmarshalledData.getIntList().get(2);
                result = bookFacility(facilityName, day, start, end);
                break;
            case "changeBooking": // Non Idempotent
                confirmID = unmarshalledData.getStringList().get(0);
                offset = unmarshalledData.getIntList().get(0);
                result = changeBooking(confirmID, offset);
                break;
            case "extendBooking": // Non Idempotent
                confirmID = unmarshalledData.getStringList().get(0);
                offset = unmarshalledData.getIntList().get(0);
                result = extendBooking(confirmID, offset);
                break;
            case "monitorFacility": // Non Idempotent
                facilityName = unmarshalledData.getStringList().get(0);
                address = unmarshalledData.getStringList().get(1);
                interval = unmarshalledData.getIntList().get(0);
                port = unmarshalledData.getIntList().get(1);
                result = monitorFacility(facilityName, address, interval, port);
                break;
            case "listFacilities": // Idempotent
                result = listFacilities();
                break;
            default: break;
        }
//        String[] stringArray = {"BookingSystemProxy", methodName, result};
//        int[] intArray = {1};
        Data data = new Data();
        data.addString("BookingSystemProxy");
        data.addString(methodName);
        data.addString(result);
        return MarshalModule.marshal(data);
    }

    public String getFacilityAvailability (String facilityName, String d){
        return bs.getFacilityAvailability(facilityName, d);
    };

    public String bookFacility (String facilityName, int d, int s, int e){
        return bs.bookFacility(facilityName, d, s, e);
    };

    public String changeBooking (String confirmID, int offset){
        return bs.changeBooking ( confirmID,  offset);
    };

    public String extendBooking (String confirmID, int offset){
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