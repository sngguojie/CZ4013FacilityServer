package com.cz4013.server;

public class BookingSystemSkeleton implements BookingSystem, RemoteObject {

    BookingSystemImpl bs;

    CommunicationModule communicationModule;

    /**
     * Takes a byte array as input, unmarshalls the byte array, and passing the arguments to the implementation of the booking system methods.
     * Returns the result from the execution of the booking system methods by marshalling the data back to a byte array
     * @param requestBody
     * @return
     */
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
        Data data = new Data();
        data.addString("BookingSystemProxy");
        data.addString(methodName);
        data.addString(result);
        return MarshalModule.marshal(data);
    }

    /**
     * Pass the arguments to the booking system implementation for getFacilityAvailability method
     * @param facilityName
     * @param daysString
     * @return
     */
    public String getFacilityAvailability (String facilityName, String daysString){
        return bs.getFacilityAvailability(facilityName, daysString);
    };

    /**
     * Pass the arguments to the booking system implementation for bookFacility method
     * @param facilityName
     * @param dayInt
     * @param startTime
     * @param endTime
     * @return
     */
    public String bookFacility (String facilityName, int dayInt, int startTime, int endTime){
        return bs.bookFacility(facilityName, dayInt, startTime, endTime);
    };

    /**
     * Pass the arguments to the booking system implementation for changeBooking method
     * @param confirmID
     * @param offset
     * @return
     */
    public String changeBooking (String confirmID, int offset){
        return bs.changeBooking ( confirmID,  offset);
    };

    /**
     * Pass the arguments to the booking system implementation for extendBooking method
     * @param confirmID
     * @param offset
     * @return
     */
    public String extendBooking (String confirmID, int offset){
        return bs.extendBooking ( confirmID,  offset);
    };

    /**
     * Pass the arguments to the booking system implementation for monitorFacility method
     * @param facilityName
     * @param address
     * @param intervalMinutes
     * @param port
     * @return
     */
    public String monitorFacility (String facilityName, String address, int intervalMinutes, int port){
        return bs.monitorFacility ( facilityName,  address,  intervalMinutes,  port);
    };

    /**
     * Pass the arguments to the booking system implementation for listFacilities method
     * @return
     */
    public String listFacilities (){
        return bs.listFacilities ();
    };

    /**
     * Sets the reference to the communication module instance
     * @param communicationModule
     */
    public void setCommunicationModule (CommunicationModule communicationModule) {
        this.communicationModule = communicationModule;
    }

    /**
     * Sets the reference to the Booking System Implementation instance
     * @param bsi
     */
    public void setBookingSystem (BookingSystemImpl bsi) {
        this.bs = bsi;
    }


}