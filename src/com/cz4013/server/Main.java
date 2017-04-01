package com.cz4013.server;

import java.io.IOException;
import java.net.InetAddress;

public class Main {

    public static void main(String[] args) throws IOException {
	// write your code here
        for (String a : args) {
//            System.out.println(a);
        }
        boolean atLeastOnce = false;
        if (args.length > 0 && args[0].contains("ATLEASTONE")) {
            atLeastOnce = true;
        }

        String remoteBinderIpAddress;
        int remoteBinderPort;
        int serverPortForRemoteBinder;

        if (args.length >= 4){
            remoteBinderIpAddress = args[1];
            remoteBinderPort = Integer.parseInt(args[2]);
            serverPortForRemoteBinder = Integer.parseInt(args[3]);
        } else {
            remoteBinderIpAddress = "192.168.1.41";
            remoteBinderPort = 2219;
            serverPortForRemoteBinder = 2220;
        }

        // set IP addresses and ports
        String[] localHostString = InetAddress.getLocalHost().toString().split("/");
        String serverIpAddress = localHostString[localHostString.length - 1];
        int serverPort = 2222;


        // instantiate remote binder comms module and object references to be stored
        RemoteBinderCommunicationModule rbcm = new RemoteBinderCommunicationModule(serverPortForRemoteBinder, remoteBinderIpAddress, remoteBinderPort);
        BookingSystemImpl bsi = new BookingSystemImpl();
        BookingSystemSkeleton bss = new BookingSystemSkeleton();

        // add BookingSystem object to remote binder table
        String remoteObjectName = "BookingSystem";
        String remoteObjectReference = serverIpAddress + "," + serverPort + "," + bsi.hashCode();
        rbcm.start();
        System.out.println(rbcm.sendAddRequest(remoteObjectName, remoteObjectReference));
        rbcm.setExit(true);

        // give server time to close udp socket
        try {
            Thread.sleep(800);
        } catch (InterruptedException e){
            e.printStackTrace();
        }

        // instantiate remaining server objects
        Binder b = new Binder();
        CommunicationModule communicationModule = new CommunicationModule(atLeastOnce);
        MonitorCallbackProxy mbp = new MonitorCallbackProxy();


        // add dependencies

        communicationModule.setPrintMessageHead(true);
        communicationModule.setLossRate(0.2f);
        communicationModule.setBinder(b);
        mbp.setCommunicationModule(communicationModule);
        bss.setCommunicationModule(communicationModule);
        bss.setBookingSystem(bsi);
        bsi.setMonitorBroadcastProxy(mbp);

        // add object to local binder
//        b.addObjectReference("MonitorCallbackProxy", mbp);
        b.addObjectReference(Integer.toString(bsi.hashCode()), bss);

        communicationModule.start();
    }
}
