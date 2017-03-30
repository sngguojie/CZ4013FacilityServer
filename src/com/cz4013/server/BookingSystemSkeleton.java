package com.cz4013.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by melvynsng on 3/30/17.
 */
public class BookingSystemSkeleton implements BookingSystem, RemoteObject {

    BookingSystemImpl bs;

    private final int MAX_BYTE_SIZE = 1024;
    private final int BYTE_CHUNK_SIZE = 4;
    private enum DATATYPE{STRING, INTEGER};
    CommunicationModule communicationModule;

    public BookingSystemSkeleton (CommunicationModule communicationModule) {
        this.bs = new BookingSystemImpl();
        this.communicationModule = communicationModule;
        this.communicationModule.addObjectReference("BookingSystemSkeleton", this);
    }

    public byte[] handleRequest (byte[] requestBody) {
        Data unmarshalledData = unmarshal(requestBody);
        String methodName = unmarshalledData.getStringList().get(0);

        String result = "Error Unrecognised";
        String facilityName, address;
        int day, start, end, confirmID, offset, interval, port;
        switch (methodName) {
            case "getFacilityAvailability": // Idempotent
                facilityName = unmarshalledData.getStringList().get(1);
                day = Integer.parseInt(unmarshalledData.getStringList().get(2));
                result = getFacilityAvailability(facilityName, day);
                break;
            case "bookFacility": // Non Idempotent
                facilityName = unmarshalledData.getStringList().get(1);
                day = Integer.parseInt(unmarshalledData.getStringList().get(2));
                start = Integer.parseInt(unmarshalledData.getStringList().get(3));
                end = Integer.parseInt(unmarshalledData.getStringList().get(4));
                result = bookFacility(facilityName, day, start, end);
                break;
            case "changeBooking": // Non Idempotent
                confirmID = Integer.parseInt(unmarshalledData.getStringList().get(1));
                offset = Integer.parseInt(unmarshalledData.getStringList().get(2));
                result = changeBooking(confirmID, offset);
                break;
            case "extendBooking": // Non Idempotent
                confirmID = Integer.parseInt(unmarshalledData.getStringList().get(1));
                offset = Integer.parseInt(unmarshalledData.getStringList().get(2));
                result = extendBooking(confirmID, offset);
                break;
            case "monitorFacility": // Non Idempotent
                facilityName = unmarshalledData.getStringList().get(1);
                address = unmarshalledData.getStringList().get(2);
                interval = Integer.parseInt(unmarshalledData.getStringList().get(3));
                port = Integer.parseInt(unmarshalledData.getStringList().get(4));
                result = monitorFacility(facilityName, address, interval, port);
                break;
            case "listFacilities": // Idempotent
                result = listFacilities();
                break;
            default: break;
        }
        String[] stringArray = {"BookingSystemProxy", result};
        return marshal(stringArray, null);
    }

    public String getFacilityAvailability (String facilityName, int d){
        return bs.getFacilityAvailability(facilityName, d);
    };

    public String bookFacility (String facilityName, int d, int s, int e){
        return bs.bookFacility(facilityName, d, s, e);
    };

    public String changeBooking (int confirmID, int offset){
        return bs.changeBooking ( confirmID,  offset);
    };

    public String extendBooking (int confirmID, int offset){
        return bs.extendBooking ( confirmID,  offset);
    };

    public String monitorFacility (String facilityName, String address, int intervalMinutes, int port){
        return bs.monitorFacility ( facilityName,  address,  intervalMinutes,  port);
    };

    public String listFacilities (){
        return listFacilities ();
    };

    public String createFacility (String facilityName){
        return createFacility ( facilityName);
    };

    public Data unmarshal(byte[] byteArray){
        int startByte = 0;
        byte[] chunk = new byte[4];
        Data data = new Data();

        while(startByte < byteArray.length){
            System.arraycopy(byteArray, startByte, chunk, 0, chunk.length);
            startByte += BYTE_CHUNK_SIZE;
            if (isEmpty(chunk)){
                break;
            }
            ByteBuffer wrapped = ByteBuffer.wrap(chunk);
            try {
                DATATYPE dataType = DATATYPE.values()[wrapped.getInt()];
                if (dataType == DATATYPE.STRING){
                    System.arraycopy(byteArray, startByte, chunk, 0, chunk.length);
                    startByte += BYTE_CHUNK_SIZE;
                    wrapped = ByteBuffer.wrap(chunk);
                    int strLength = wrapped.getInt();
                    String str = "";
                    for (int i = 0; i < strLength; i+=4){
                        System.arraycopy(byteArray, startByte, chunk, 0, chunk.length);
                        startByte += BYTE_CHUNK_SIZE;
                        str += new String(chunk);
                    }
                    data.addString(str);
                } else if (dataType == DATATYPE.INTEGER){
                    System.arraycopy(byteArray, startByte, chunk, 0, chunk.length);
                    startByte += BYTE_CHUNK_SIZE;
                    wrapped = ByteBuffer.wrap(chunk);
                    int i = wrapped.getInt();
                    data.addInt(i);
                }
            } catch (Exception e){
                System.out.println("Data issue.");
            }

        }

        return data;
    }

    private boolean isEmpty(byte[] byteArray){
        boolean empty = true;
        for (byte b : byteArray){
            if (b != 0){
                empty = false;
            }
        }
        return empty;
    }

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

}