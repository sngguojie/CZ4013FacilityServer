package com.cz4013.server;

import java.io.*;
import java.net.*;
import java.util.*;

public class FacilityBookingServer extends Thread {

    protected DatagramSocket socket = null;
    protected boolean isRunning = true;
    protected HashMap<String, String> req2res = new HashMap<String, String>();


    public FacilityBookingServer () throws IOException {
        // PORT 2222 is default for NTU computers
        this("FacilityBookingServer", 2222);
    }

    public FacilityBookingServer (String name, int PORT) throws IOException {
        super(name);
        socket = new DatagramSocket(new InetSocketAddress(PORT));
        initialise();
        System.out.println("Facility Booking Server Initialized");
        System.out.println(socket.getLocalAddress().getHostAddress());
    }

    public void run () {
        System.out.println("Facility Booking Server Run");
        while (this.isRunning) {
            try {
                byte[] buf = new byte[256];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                String request = new String(packet.getData());
                System.out.println(request);
                String response = executeRequest(request, address, port);
                System.out.println(response);
                buf = response.getBytes();
                packet = new DatagramPacket(buf, buf.length, address, port);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
                isRunning = false;
            }
        }
        socket.close();
    }

    public void broadcast (String facilityName) throws IOException {
        System.out.println("Broadcast " + facilityName);
        String availability = "0 0 Broadcast " + getFacilityWeekAvailability(facilityName);
        byte[] buf = availability.getBytes();
        System.out.println(Facility.facilityHashMap.get(facilityName).monitorList.size());
        for (Monitor m : Facility.facilityHashMap.get(facilityName).monitorList) {
            InetAddress address = m.address;
            int port = m.port;
            long expiry = m.expiry;
            long now = System.currentTimeMillis();
            System.out.println(Long.toString(expiry) + " " + Long.toString(now));
            if (now > expiry) {
                Facility.facilityHashMap.get(facilityName).monitorList.remove(m);
            } else {
                DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
                socket.send(packet);
            }
        }


    }

    private String executeRequest(String request, InetAddress address, int port) throws IOException  {
        String[] requestArray = request.split(" ");
        String result = "Error Unrecognised " + request;
        if (requestArray[0].equals("0")) {
            String requestID = requestArray[1];
            String facilityName;
            int day, start, end, confirmID, offset, interval;
            switch (requestArray[2]) {
                case "Get": // Idempotent
                    facilityName = requestArray[3];
                    day = Integer.parseInt(requestArray[4]);
                    result = getFacilityAvailability(facilityName, day);
                    break;
                case "Book": // Non Idempotent
                    if (req2res.containsKey(request)) {
                        result = req2res.get(request);
                        break;
                    }
                    facilityName = requestArray[3];
                    day = Integer.parseInt(requestArray[4]);
                    start = Integer.parseInt(requestArray[5]);
                    end = Integer.parseInt(requestArray[6]);
                    result = bookFacility(facilityName, day, start, end);
                    req2res.put(request,result);
                    break;
                case "Change": // Non Idempotent
                    if (req2res.containsKey(request)) {
                        result = req2res.get(request);
                        break;
                    }
                    confirmID = Integer.parseInt(requestArray[3]);
                    offset = Integer.parseInt(requestArray[4]);
                    result = changeBooking(confirmID, offset);
                    req2res.put(request,result);
                    break;
                case "Monitor": // Non Idempotent
                    if (req2res.containsKey(request)) {
                        result = req2res.get(request);
                        break;
                    }
                    facilityName = requestArray[3];
                    interval = Integer.parseInt(requestArray[4]);
                    result = monitorFacility(facilityName, interval, address, port);
                    req2res.put(request,result);
                    break;
                case "List": // Idempotent
                    result = listFacilities();
                    break;
                case "Create": // Idempotent
                    facilityName = requestArray[3];
                    result = createFacility(facilityName);
                    break;
                default: break;
            }

            return "1 " + requestID + " " + result;
        }
        return result;
    }

    private void initialise () {
        Facility meetingRoom1 = new Facility("MeetingRoom1");
        meetingRoom1.save();
        Facility LectureTheatre1 = new Facility("LectureTheatre1");
        LectureTheatre1.save();
    }

    private Facility.DAYS getDay (int d) {
        Facility.DAYS day = null;
        switch (d) {
            case 0: day = Facility.DAYS.MON; break;
            case 1: day = Facility.DAYS.TUE; break;
            case 2: day = Facility.DAYS.WED; break;
            case 3: day = Facility.DAYS.THU; break;
            case 4: day = Facility.DAYS.FRI; break;
            case 5: day = Facility.DAYS.SAT; break;
            case 6: day = Facility.DAYS.SUN; break;
            default: break;
        }
        return day;
    }


    public String getFacilityAvailability (String facilityName, int d) {
        if (!Facility.facilityHashMap.containsKey(facilityName)) {
            return "Error Get Unrecognised FacilityName " + facilityName;
        }
        Facility.DAYS day = getDay(d);
        if (day == null) {
            return "Error Get Unrecognised Day " + Integer.toString(d);
        }
        Facility facility = Facility.facilityHashMap.get(facilityName);
        ArrayList<Booking> bookings = facility.bookings.get(day);
        String result = "Success Get " + facilityName + " " + getDayString(day) + " ";
        for (Booking b : bookings) {
            result += b.toString() + " ";
        }
        return result;
    }

    public String bookFacility (String facilityName, int d, int s, int e) throws IOException {
        if (!Facility.facilityHashMap.containsKey(facilityName)) {
            return "Error Book FacilityName " + facilityName;
        }
        Facility.DAYS day = getDay(d);
        if (day == null) {
            return "Error Book Day " + Integer.toString(d);
        }
        Facility facility = Facility.facilityHashMap.get(facilityName);
        ArrayList<Booking> bookings = facility.bookings.get(day);
        Booking temp = new Booking(s, e, day, facility);
        if (!temp.isValid()) {
            return ("Error Book Time " + Integer.toString(s) + "-" + Integer.toString(e));
        }
        boolean isConflict = false;
        Booking conflictedBooking = null;
        for (Booking b : bookings) {
            if (temp.conflict(b)) {
                isConflict = true;
                conflictedBooking = b;
            }
        }
        if (isConflict) {
            return "Error Book Conflict " + temp.toString() + " " + conflictedBooking.toString();
        }
        bookings.add(temp);
        Confirmation confirmation = new Confirmation(temp);
        int confirmationID = confirmation.save();
        broadcast(facilityName);
        return "Success Book ConfirmID " + Integer.toString(confirmationID);
    }

    public String changeBooking (int confirmID, int offset) throws IOException {
        if (!Confirmation.confirmationHashMap.containsKey(confirmID)) {
            return "Error Change ConfirmID " + Integer.toString(confirmID);
        }
        Confirmation confirmation = Confirmation.confirmationHashMap.get(confirmID);
        Booking initial = confirmation.booking;
        Booking copy = new Booking(initial.start + offset, initial.end + offset, initial.day, initial.facility);
        if (!copy.isValid()) {
            return "Error Change Offset " + Integer.toString(offset);
        }
        ArrayList<Booking> bookings = initial.facility.bookings.get(initial.day);
        boolean isConflict = false;
        Booking conflictedBooking = null;
        for (Booking b : bookings) {
            if (!b.equals(initial)) {
                if (copy.conflict(b)) {
                    isConflict = true;
                    conflictedBooking = b;
                }
            }
        }
        if (isConflict) {
            return "Error Change Conflict " + copy.toString() + " " + conflictedBooking.toString();
        }
        confirmation.booking = copy;
        bookings.remove(initial);
        bookings.add(copy);
        broadcast(copy.facility.name);
        return "Success Change ConfirmID " + Integer.toString(confirmID) + " " + copy.toString();
    }

    public String monitorFacility (String facilityName, int intervalMinutes, InetAddress address, int port) {
        if (!Facility.facilityHashMap.containsKey(facilityName)) {
            return "Error Monitor FacilityName " + facilityName;
        }
        Facility facility = Facility.facilityHashMap.get(facilityName);
        long now = System.currentTimeMillis();
        long expiry = now + ((long) intervalMinutes) * 60000l;
        Monitor monitor = new Monitor(address, port, expiry);
        facility.monitorList.add(monitor);
        return "Success Monitor";
    }

    public String listFacilities () {
        String result = "Success List";
        for (Facility f : Facility.facilityHashMap.values()) {
            result += " " + f.name;
            System.out.println(f.name);
        }
        return result;
    }

    public String createFacility (String facilityName) {
        Facility newFacility = new Facility(facilityName);
        newFacility.save();
        return "Success Created facility " + facilityName;
    }

    private String getDayString (Facility.DAYS day) {
        switch (day) {
            case MON: return "MON";
            case TUE: return "TUE";
            case WED: return "WED";
            case THU: return "THU";
            case FRI: return "FRI";
            case SAT: return "SAT";
            case SUN: return "SUN";
            default: return null;
        }
    }

    public String getFacilityWeekAvailability (String facilityName) {
        if (!Facility.facilityHashMap.containsKey(facilityName)) {
            return "Error Unrecognised FacilityName " + facilityName;
        }
        Facility facility = Facility.facilityHashMap.get(facilityName);
        String result = facility.name + "\n";
        for (Facility.DAYS d : facility.bookings.keySet()) {
            String dayString = getDayString(d);
            result += dayString + " ";
            for (Booking b : facility.bookings.get(d)) {
                result += b.toString() + " ";
            }
            result += "\n";
        }
        return result;
    }
}
