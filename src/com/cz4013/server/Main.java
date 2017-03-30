package com.cz4013.server;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
	// write your code here
        Binder b = new Binder();
        CommunicationModule communicationModule = new CommunicationModule();
        communicationModule.setBinder(b);
        new BookingSystemSkeleton(communicationModule);
        new MonitorBroadcastProxy(communicationModule);
        communicationModule.start();
    }
}
