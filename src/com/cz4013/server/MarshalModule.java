package com.cz4013.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by danielseetoh on 3/31/17.
 */
public class MarshalModule {

    static final int MAX_BYTE_SIZE = 1020;
    static final int BYTE_CHUNK_SIZE = 4;
    private enum DATATYPE{STRING, INTEGER};

    public static byte[] marshal(String[] strings, int[] ints){
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

    public static Data unmarshal(byte[] byteArray){
        System.out.println("unmarshal");
        int startByte = 0;
        byte[] chunk = new byte[4];
        Data data = new Data();
        String objectReference = null;
        String methodId = null;

        while(startByte < byteArray.length){
            System.out.println("while");
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
                    if (objectReference == null){
                        objectReference = str;
                        data.setObjectReference(objectReference);
                    } else if (methodId == null){
                        methodId = str;
                        data.setMethodId(methodId);
                    } else {
                        data.addString(str);
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

    public static boolean isEmpty(byte[] byteArray){
        System.out.println("isEmpty");
        boolean empty = true;
        for (byte b : byteArray){
            if (b != 0){
                empty = false;
            }
        }
        System.out.println(empty);
        return empty;
    }

    public static int incrementByteIndex(int index){
        return index += BYTE_CHUNK_SIZE-(index%BYTE_CHUNK_SIZE);
    }


    public static byte[] addIntToByteArray(byte[] byteArray, int i, int startIndex){
        byte[] intByteArray = ByteBuffer.allocate(BYTE_CHUNK_SIZE).putInt(i).array();
        System.arraycopy(intByteArray, 0, byteArray, startIndex, intByteArray.length);
        return byteArray;
    }
}



