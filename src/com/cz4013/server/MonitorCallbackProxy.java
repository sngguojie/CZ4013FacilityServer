package com.cz4013.server;

import java.util.ArrayList;

/**
 * Created by melvynsng on 3/30/17.
 */
public class MonitorCallbackProxy implements MonitorCallback, RemoteObject {

    CommunicationModule communicationModule;
    ArrayList<Monitor> expired = new ArrayList<Monitor>();
    public MonitorCallbackProxy() {
    }

    /**
     * To send the requests to display the availability of a facility to all clients registered to monitor the facility
     * @param facilityName
     */
    public void displayAvailability(String facilityName) {
        String availability = "";

        Facility facility = Facility.facilityHashMap.get(facilityName);
        availability += facility.getWeekAvailability();

        for (Monitor m : facility.monitorList) {
            if (expired.contains(m)) {}
            if (m.expiry < System.currentTimeMillis()) {
                availability = "Expired";
            }
            Data data = new Data();
            data.addString("MonitorCallbackSkeleton");
            data.addString("displayAvailability");
            data.addString(availability);
            byte[] marshalledBytes = MarshalModule.marshal(data);

            communicationModule.sendRequest(false,marshalledBytes, m.address, m.port);
            if (availability.equals("Expired")) {
                expired.add(m);
            }
        }

    }

    /**
     * To handle any request sent to the Monitor Callback Proxy Instance
     * @param requestBody
     * @return
     */
    public byte[] handleRequest (byte[] requestBody) {
        return null;
    };

    /**
     * To set the reference to the communication module
     * @param communicationModule
     */
    public void setCommunicationModule (CommunicationModule communicationModule) {
        this.communicationModule = communicationModule;
    }

}
