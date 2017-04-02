package com.cz4013.server;


import java.nio.ByteBuffer;

/**
 * Created by danielseetoh on 3/31/17.
 */
public class MarshalModule {
    static final int MAX_BYTE_SIZE = 1020;
    static final int BYTE_CHUNK_SIZE = 4;
    private enum DATATYPE{STRING, INTEGER};


    /**
     * MessageType 1 byte, Idempotence, 1 byte, requestId 2 bytes, type of object ref (string 0) 4 bytes, Length of object ref 4 bytes
     * Object ref 4 bytes, type of method id (string 0) 4 bytes, length of method id 4 bytes, method id 4 bytes,
     * type of string (0) 4 bytes, length of string 4 bytes, string chunks of 4 bytes
     * type of int (1) 4 bytes, int chunks of 4 bytes
     * java is big-endian by default.
     * network byte order is big-endian as well.
     *
     * This function handles only object references, method IDs, string arguments and integer arguments.
     * The messageType, idempotence and requestID is handled by the communication module.
     * @param data
     * @return
     */
    public static byte[] marshal(Data data){

        byte[] outBuf = new byte[MAX_BYTE_SIZE];

        // string types are defined as 1, and integer types are defined as 2. This is prior knowledge required by the marshalling
        // and unmarshalling modules of both client and server.
        int startByte = 0;
        int strType = 1;
        int intType = 2;

        // for each string, put in the string type (1), then the string length, and then the string, all of these are
        // flushed to multiples of 4 bytes.
        for (String str : data.getStringList()){
            int strLength = str.length();
            outBuf = addIntToByteArray(outBuf, strType, startByte);
            startByte = incrementByteIndex(startByte);

            outBuf = addIntToByteArray(outBuf, strLength, startByte);
            startByte = incrementByteIndex(startByte);

            char[] ch = str.toCharArray();
            for (int i = 0; i < ch.length; i++){
                outBuf[startByte] = (byte)ch[i];
                startByte++;
                if (i == ch.length-1){
                    startByte--;
                }
            }

            startByte = incrementByteIndex(startByte);
        }

        // for each integer, put in the integer type (2), then the integer. All of these are flushed to multiples of 4 bytes.
        for (int i : data.getIntList()){
            outBuf = addIntToByteArray(outBuf, intType, startByte);
            startByte = incrementByteIndex(startByte);

            outBuf = addIntToByteArray(outBuf, i, startByte);
            startByte = incrementByteIndex(startByte);
        }
        return outBuf;
    }

    /**
     * Unmarshals the byteArray into a Data object. This byteArray is assumed to be stripped of the messageType, idempotence,
     * and requestId bytes. It will handle the object reference, methodID, string arguments and integer arguments.
     * @param byteArray
     * @return
     */
    public static Data unmarshal(byte[] byteArray){

        int startByte = 0;
        byte[] chunk = new byte[4];
        Data data = new Data();
        String objectReference = null;
        String methodId = null;

        // Runs through the entire message
        while(startByte < byteArray.length){
            System.arraycopy(byteArray, startByte, chunk, 0, chunk.length);
            startByte += BYTE_CHUNK_SIZE;

            // Breaks early if it meets a chunk of 4 bytes which is empty, signalling
            // that there are no more arguments to be handled
            if (isEmpty(chunk)){
                break;
            }
            ByteBuffer wrapped = ByteBuffer.wrap(chunk);

            try {
                // get the datatype, which is an enum of either STRING or INTEGER
                DATATYPE dataType = DATATYPE.values()[wrapped.getInt()-1];

                // if the datatype is string, check for length of string, then get the string
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
                    str = str.substring(0, strLength);

                    // if the objectreference is not yet set, set that first, as the object reference always comes first
                    // if not, check if methodID is set as the methodID always comes second,
                    // if not, it is a normal string argument and is stored in the string list of the data object
                    if (objectReference == null){
                        objectReference = str;
                        data.setObjectReference(objectReference);
                    } else if (methodId == null){
                        methodId = str;
                        data.setMethodId(methodId);
                    } else {
                        data.addString(str);
                    }

                // if the datatype is integer, just read the integer
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

    /**
     * checks if the chunk of bytes are empty
     * @param byteArray
     * @return
     */
    public static boolean isEmpty(byte[] byteArray){
        boolean empty = true;
        for (byte b : byteArray){
            if (b != 0){
                empty = false;
            }
        }
        return empty;
    }

    /**
     * increments the index to the next multiple of 4
     * @param index
     * @return
     */
    public static int incrementByteIndex(int index){
        return index += BYTE_CHUNK_SIZE-(index%BYTE_CHUNK_SIZE);
    }

    /**
     * insert integer into the byte array at a specified index
     * @param byteArray
     * @param i
     * @param startIndex
     * @return
     */
    public static byte[] addIntToByteArray(byte[] byteArray, int i, int startIndex){
        byte[] intByteArray = ByteBuffer.allocate(BYTE_CHUNK_SIZE).putInt(i).array();
        System.arraycopy(intByteArray, 0, byteArray, startIndex, intByteArray.length);
        return byteArray;
    }
}



