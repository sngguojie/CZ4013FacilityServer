package com.cz4013.server;


import java.io.*;
import java.net.*;
import java.util.*;

public class FacilityBookingServer extends Thread {

    protected boolean isRunning = true;
    protected DatagramSocket socket = null;
    protected HashMap<ClientRequest, byte[]> responseHistory = new HashMap<ClientRequest, byte[]>();
    protected HashMap<Byte, RemoteObject> objectReference = new HashMap<Byte, RemoteObject>();

    public FacilityBookingServer () throws IOException {
        // PORT 2222 is default for NTU computers
        this("FacilityBookingServer", 2222);
    }

    public FacilityBookingServer (String name, int PORT) throws IOException {
        super(name);
        socket = new DatagramSocket(new InetSocketAddress(PORT));
        objectReference.put((byte) 1, new BookingSystemSkeleton());
    }

    public void run () {
        System.out.println("Facility Booking Server Running");
        while (this.isRunning) {
            try {
                byte[] buf = new byte[256];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                byte[] response = handleRequest(packet.getData(), address, port);
                if (response == null) {
                    continue;
                }
                buf = response;
                packet = new DatagramPacket(buf, buf.length, address, port);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
                isRunning = false;
            }
        }
        socket.close();
    }

    private byte[] handleRequest(byte[] requestBytes, InetAddress address, int port) throws IOException {
        byte messageType = requestBytes[0];
        byte requestId = requestBytes[1];
        byte[] requestBody = Arrays.copyOfRange(requestBytes, 2, requestBytes.length);

        byte[] responseHead = new byte[2];
        byte[] responseBody = null;
        if (messageType == 0) { // idempotent request
            responseHead[0] = 1;
            responseHead[1] = requestId;
            responseBody = getResponseBody(requestBody, address, port);
            byte[] response = new byte[responseHead.length + responseBody.length];
            System.arraycopy(responseHead, 0, response, 0, responseHead.length);
            System.arraycopy(responseBody, 0, response, responseHead.length, responseBody.length);
            return response;
        } else if (messageType == 1) { // non idempotent request
            responseHead[0] = 1;
            responseHead[1] = requestId;
            ClientRequest cr = new ClientRequest(address, port, requestId);
            if (responseHistory.containsKey(cr)) {
                responseBody = responseHistory.get(cr);
            } else {
                responseBody = getResponseBody(requestBody, address, port);
                responseHistory.put(cr, responseBody);
            }
            byte[] response = new byte[responseHead.length + responseBody.length];
            System.arraycopy(responseHead, 0, response, 0, responseHead.length);
            System.arraycopy(responseBody, 0, response, responseHead.length, responseBody.length);
            return response;
        }
        return null;
    }

    private byte[] getResponseBody (byte[] requestBody, InetAddress address, int port) {
        byte objectRef = requestBody[0];
        objectRef = 1;
        RemoteObject remoteObject = objectReference.get(objectRef);
        return remoteObject.handleRequest(Arrays.copyOfRange(requestBody,1,requestBody.length), address, port);
    }

    private void sendRequest (byte[] request, InetAddress address, int port) throws IOException {
        byte[] buf = request;
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
        socket.send(packet);
    }

//    public void broadcast (String facilityName) throws IOException {
//        System.out.println("Broadcast " + facilityName);
//        String availability = "0 0 Broadcast " + bookingSystem.getFacilityWeekAvailability(facilityName);
//        byte[] buf = availability.getBytes();
//        System.out.println(Facility.facilityHashMap.get(facilityName).monitorList.size());
//        for (Monitor m : Facility.facilityHashMap.get(facilityName).monitorList) {
//            InetAddress address = m.address;
//            int port = m.port;
//            long expiry = m.expiry;
//            long now = System.currentTimeMillis();
//            System.out.println(Long.toString(expiry) + " " + Long.toString(now));
//            if (now > expiry) {
//                Facility.facilityHashMap.get(facilityName).monitorList.remove(m);
//            } else {
//                sendRequest(availability.getBytes(), address, port);
//            }
//        }
//    }
}
