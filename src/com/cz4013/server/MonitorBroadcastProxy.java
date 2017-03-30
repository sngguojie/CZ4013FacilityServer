package com.cz4013.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by melvynsng on 3/30/17.
 */
public class MonitorBroadcastProxy implements MonitorBroadcast, RemoteObject {

    final int MAX_BYTE_SIZE = 1024;
    final int BYTE_CHUNK_SIZE = 4;
    CommunicationModule communicationModule;

    public MonitorBroadcastProxy () {
    }

    public void displayAvailability(String facilityName) {
        String availability = "";

        Facility facility = Facility.facilityHashMap.get(facilityName);
        availability += facility.getWeekAvailability();

        for (Monitor m : facility.monitorList) {
            if (m.expiry > System.currentTimeMillis()) {
                availability = "Expired";
            }
            String[] data = {"MonitorBroadcastSkeleton", "displayAvailability", availability};
            int[] intArr = {1};
            byte[] marshalledBytes = marshal(data, intArr);

            communicationModule.sendRequest(marshalledBytes, m.address, m.port);
        }




    }

    public byte[] handleRequest (byte[] requestBody) {
        return null;
    };


    private byte[] marshal(String[] strings, int[] ints){
        // MessageType 2 bytes, requestId 2 bytes, type of object ref (string 0) 4 bytes, Length of object ref 4 bytes
        // Object ref 4 bytes, type of string (0) 4 bytes, length of string 4 bytes, string chunks of 4 bytes
        // type of int (1) 4 bytes, int chunks of 4 bytes

        // java is big-endian by default
        // network byte order is big-endian as well

        byte[] outBuf = new byte[MAX_BYTE_SIZE];

        // leave space for messageType and requestId to be filled in by communication module
        int startByte = 0;
        int strType = 0;
        int strTypePadding = 3;
        int intType = 1;
        int intTypePadding = 3;

        for (String str : strings){
            int strLength = str.length();
            outBuf = addIntToByteArray(outBuf, strType, startByte);
            startByte = incrementByteIndex(startByte);

            outBuf = addIntToByteArray(outBuf, strLength, startByte);
            startByte = incrementByteIndex(startByte);

            char[] ch = str.toCharArray();
            for (int i = 0; i < ch.length; i++){
                outBuf[startByte] = (byte)ch[i];
                startByte++;
            }

            startByte = incrementByteIndex(startByte);
        }

        for (int i : ints){
            outBuf = addIntToByteArray(outBuf, intType, startByte);
            startByte = incrementByteIndex(startByte);

            outBuf = addIntToByteArray(outBuf, i, startByte);
            startByte = incrementByteIndex(startByte);
        }

        System.out.println(new String(outBuf));

        return outBuf;
    }

    private int incrementByteIndex(int index){
        return index += BYTE_CHUNK_SIZE-(index%BYTE_CHUNK_SIZE);
    }

    private byte[] addIntToByteArray(byte[] byteArray, int i, int startIndex){
        byte[] intByteArray = ByteBuffer.allocate(BYTE_CHUNK_SIZE).putInt(i).array();
        System.arraycopy(intByteArray, 0, byteArray, startIndex, intByteArray.length);
        return byteArray;
    }

    public void setCommunicationModule (CommunicationModule cm) {
        this.communicationModule = cm;
    }

}
