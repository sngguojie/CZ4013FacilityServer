package com.cz4013.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by melvynsng on 3/30/17.
 */
public class MonitorCallbackProxy implements MonitorBroadcast, RemoteObject {

    CommunicationModule communicationModule;

    public MonitorCallbackProxy() {
    }

    public void displayAvailability(String facilityName) {
        String availability = "";

        Facility facility = Facility.facilityHashMap.get(facilityName);
        availability += facility.getWeekAvailability();

        for (Monitor m : facility.monitorList) {
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
                facility.monitorList.remove(m);
            }
        }

    }

    public byte[] handleRequest (byte[] requestBody) {
        return null;
    };

    public void setCommunicationModule (CommunicationModule cm) {
        this.communicationModule = cm;
    }

}
