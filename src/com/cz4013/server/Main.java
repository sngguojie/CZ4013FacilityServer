package com.cz4013.server;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
	// write your code here
        Binder b = new Binder();
        CommunicationModule communicationModule = new CommunicationModule();
        BookingSystemSkeleton bss = new BookingSystemSkeleton();
        MonitorBroadcastProxy mbp = new MonitorBroadcastProxy();
        BookingSystemImpl bsi = new BookingSystemImpl();

        communicationModule.setBinder(b);
        mbp.setCommunicationModule(communicationModule);
        bss.setCommunicationModule(communicationModule);

        bss.setBookingSystem(bsi);
        b.addObjectReference("MonitorBroadcastProxy", mbp);
        b.addObjectReference("BookingSystemSkeleton", bss);


        communicationModule.start();
    }
}
