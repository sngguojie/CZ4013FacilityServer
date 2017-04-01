package com.cz4013.server;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
	// write your code here
        for (String a : args) {
            System.out.println(a);
        }
        boolean saveHistory = true;
        if (args.length > 0 && args[0].contains("ATLEASTONE")) {
            saveHistory = false;
        }

        Binder b = new Binder();
        CommunicationModule communicationModule = new CommunicationModule(saveHistory);
        BookingSystemSkeleton bss = new BookingSystemSkeleton();
        MonitorBroadcastProxy mbp = new MonitorBroadcastProxy();
        BookingSystemImpl bsi = new BookingSystemImpl();

        communicationModule.setBinder(b);
        mbp.setCommunicationModule(communicationModule);
        bss.setCommunicationModule(communicationModule);

        bss.setBookingSystem(bsi);
        bsi.setMonitorBroadcastProxy(mbp);

        b.addObjectReference("MonitorBroadcastProxy", mbp);
        b.addObjectReference("BookingSystemSkeleton", bss);



        communicationModule.start();
    }
}
