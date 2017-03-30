package com.cz4013.server;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;

public class CommunicationModule extends Thread {
    protected boolean isRunning = true;
    protected DatagramSocket socket = null;
    protected HashMap<byte[], byte[]> messageHistory = new HashMap<byte[], byte[]>();
    protected enum MSGTYPE {IDEMPOTENT_REQUEST, NON_IDEMPOTENT_REQUEST, IDEMPOTENT_RESPONSE, NON_IDEMPOTENT_RESPONSE};
    protected enum DATATYPE {STRING, INT};
    protected InetAddress serverAddress;
    protected int serverPort;
    protected HashMap<Integer, byte[]> requestHistory = new HashMap<Integer,byte[]>();
    private Binder binder;
    private final int MAX_BYTE_SIZE = 1024;

    public CommunicationModule() throws IOException {
        // PORT 2222 is default for NTU computers
        this("CommunicationModule", 2222);
    }

    public CommunicationModule(String name, int PORT) throws IOException {
        super(name);
        socket = new DatagramSocket(new InetSocketAddress(PORT));
//        serverPort = PORT;
        String[] localHostString = InetAddress.getLocalHost().toString().split("/");
        System.out.println(localHostString[localHostString.length - 1]);
        serverAddress = InetAddress.getByName(localHostString[localHostString.length - 1]);
    }

    public void run () {
        System.out.println("CommunicationModule Running");
        while (this.isRunning) {
            try {
                byte[] buf = new byte[MAX_BYTE_SIZE];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                handlePacketIn(packet.getData(), address, port);
            } catch (IOException e) {
                e.printStackTrace();
                isRunning = false;
            }
        }
        socket.close();
    }

    private MSGTYPE getMessageType (byte messageTypeByte, byte idempotentTypeByte) {
        if (messageTypeByte == (byte)0x00 && idempotentTypeByte == (byte)0x00) {
            return MSGTYPE.IDEMPOTENT_REQUEST; // Request
        }
        if (messageTypeByte == (byte)0x00 && idempotentTypeByte == (byte)0x01) {
            return MSGTYPE.NON_IDEMPOTENT_REQUEST;
        }
        if (messageTypeByte == (byte)0x01 && idempotentTypeByte == (byte)0x00) {
            return MSGTYPE.IDEMPOTENT_RESPONSE;
        }
        if (messageTypeByte == (byte)0x01 && idempotentTypeByte == (byte)0x01) {
            return MSGTYPE.NON_IDEMPOTENT_RESPONSE;
        }
        return null;
    }

    private byte[] getMessageTypeAsBytes (MSGTYPE messageType) {
        byte messageTypeByte = (byte)0x00;
        byte idempotentTypeByte = (byte)0x00;
        byte[] result = new byte[2];
        switch (messageType) {
            case IDEMPOTENT_REQUEST:
                messageTypeByte = (byte)0x00;
                idempotentTypeByte = (byte)0x00;
                break;
            case NON_IDEMPOTENT_REQUEST:
                messageTypeByte = (byte)0x00;
                idempotentTypeByte = (byte)0x01;
                break;
            case IDEMPOTENT_RESPONSE:
                messageTypeByte = (byte)0x01;
                idempotentTypeByte = (byte)0x00;
                break;
            case NON_IDEMPOTENT_RESPONSE:
                messageTypeByte = (byte)0x01;
                idempotentTypeByte = (byte)0x01;
                break;
            default:
                break;
        }
        result[0] = messageTypeByte;
        result[1] = idempotentTypeByte;
        return result;

    }

    private byte[] getResponseHead (MSGTYPE messageType, int requestId) {
        byte[] requestIdBytes = getHalfWordAsBytes(requestId);
        byte[] messageTypeBytes = getMessageTypeAsBytes(messageType);
        byte[] result = new byte[4];
        System.arraycopy(messageTypeBytes, 0, result, 0, 2);
        System.arraycopy(requestIdBytes, 0, result, 2, 2);
        return result;
    }

    private int getBytesAsHalfWord (byte[] bytes) {
        return ((bytes[0] & 0xff) << 8) | (bytes[1] & 0xff);
    }

    private byte[] getHalfWordAsBytes (int halfword) {
        byte[] data = new byte[2];
        data[0] = (byte) (halfword & 0xFF);
        data[1] = (byte) ((halfword >> 8) & 0xFF);
        return data;
    }

    private byte[] combineByteArrays (byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    private DATATYPE getDataType (byte[] word) {
        if (word[0] == (byte)0x00) {
            return DATATYPE.STRING;
        }
        if (word[0] == (byte)0x01) {
            return DATATYPE.INT;
        }
        return null;
    }

    private RemoteObject getRemoteObject (byte[] payload) {

        System.out.println(new String(payload));
        DATATYPE dataType = getDataType(Arrays.copyOfRange(payload, 0, 4));
        if (dataType != DATATYPE.STRING) {
            return null;
        }
        int stringLen = ByteBuffer.wrap(Arrays.copyOfRange(payload, 4, 8)).getInt();
//        stringLen += 4 - (stringLen % 4);
        String objectRefName = new String(Arrays.copyOfRange(payload, 8, 8 + stringLen));

        System.out.println(objectRefName);
        System.out.println(binder.getObjectReference(objectRefName).toString());
        return binder.getObjectReference(objectRefName);
    }

    private void handlePacketIn(byte[] payload, InetAddress address, int port) throws IOException {
        byte messageTypeByte = payload[0];
        byte idempotentTypeByte = payload[1];
        byte[] requestIdBytes = new byte[2];
        System.arraycopy(payload, 2, requestIdBytes, 0, 2);

        MSGTYPE messageType = getMessageType(messageTypeByte, idempotentTypeByte);
        int requestId = getBytesAsHalfWord(requestIdBytes);
        byte[] inHead = Arrays.copyOfRange(payload, 0, 4);
        byte[] inBody = Arrays.copyOfRange(payload, 4, payload.length);
        byte[] outHead, outBody, out;

        switch (messageType) {
            case IDEMPOTENT_REQUEST:
                outHead = getResponseHead(MSGTYPE.IDEMPOTENT_RESPONSE, requestId);
                outBody = getRemoteObjectResponse(inBody);
                out = combineByteArrays(outHead, outBody);
                sendReponsePacketOut(out, address, port);
                break;
            case NON_IDEMPOTENT_REQUEST:
                if (messageHistory.containsKey(inHead)) {
                    out = messageHistory.get(inHead);
                    sendReponsePacketOut(out, address, port);
                    break;
                }
                outHead = getResponseHead(MSGTYPE.IDEMPOTENT_RESPONSE, requestId);
                outBody = getRemoteObjectResponse(inBody);
                out = combineByteArrays(outHead, outBody);
                messageHistory.put(inHead,out);
                sendReponsePacketOut(out, address, port);
                break;
            case IDEMPOTENT_RESPONSE:
                if (messageHistory.containsKey(inHead)) {
                    break;
                }
                messageHistory.put(inHead, inBody);
                getRemoteObjectResponse(inBody);
                break;
            case NON_IDEMPOTENT_RESPONSE:
                if (messageHistory.containsKey(inHead)) {
                    break;
                }
                messageHistory.put(inHead, inBody);
                getRemoteObjectResponse(inBody);
                break;
            default:
                break;
        }

    }

    private byte[] getRemoteObjectResponse (byte[] requestBody) {
        System.out.println(MarshalModule.unmarshal(requestBody).toString());
        RemoteObject remoteObject = getRemoteObject(requestBody);
        return remoteObject.handleRequest(Arrays.copyOfRange(requestBody,0,requestBody.length));
    }

    private int getNewRequestId () {
        int i = 0;
        while (requestHistory.containsKey(i)) {
            i++;
        }
        return i;
    }


    public byte[] sendRequest(byte[] data) {
        return sendRequest(data, serverAddress, serverPort);
    }

    public void sendResponse(byte[] data) {
        sendResponse(data, serverAddress, serverPort);
    }

    public byte[] sendRequest(byte[] data, InetAddress address, int port) {
        System.out.println("sendRequest");
        try {
            byte[] payload = makePayload(data);
            return sendRequestPacketOut(payload, address, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void sendResponse(byte[] data, InetAddress address, int port) {
        try {
            byte[] payload = makePayload(data);
            sendReponsePacketOut(payload, address, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] makePayload (byte[] data) throws IOException {
        int requestIdInt = getNewRequestId();
        byte[] outHead = getResponseHead(MSGTYPE.IDEMPOTENT_REQUEST, requestIdInt);
        byte[] out = combineByteArrays(outHead, data);
        requestHistory.put(requestIdInt,out);
        return out;
    }

    private void sendReponsePacketOut (byte[] payload, InetAddress address, int port) throws IOException {
        if (payload == null) {
            return;
        }
        // send request
        byte[] buf = payload;
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
        socket.send(packet);
    }

    private byte[] sendRequestPacketOut (byte[] payload, InetAddress address, int port) throws IOException {
        if (payload == null) {
            return null;
        }
        System.out.println("sendRequestPacketOut");
        boolean resend = true;
        byte[] requestIdBytesOut = new byte[2];
        System.arraycopy(payload, 2, requestIdBytesOut, 0, 2);
        int requestIdOut = getBytesAsHalfWord(requestIdBytesOut);
        // send request
        do {
            try {
                byte[] buf = payload;

                //Debug
                System.out.println(new String(buf));
                byte[] temp = Arrays.copyOfRange(buf,4,buf.length-4);
                System.out.println(new String(temp));
                System.out.println(MarshalModule.unmarshal(temp).toString());
                //endDebug

                DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
                System.out.println("b4socket.setSoTimeout(5000);");
//                socket.setSoTimeout(5000);
                System.out.println("socket.setSoTimeout(5000);");
                socket.send(packet);

                byte[] bufIn = new byte[MAX_BYTE_SIZE];
                packet = new DatagramPacket(bufIn, bufIn.length);
                System.out.println("b4socket.receive(packet)");
                socket.receive(packet);
                System.out.println("socket.receive(packet)");
                InetAddress addressIn = packet.getAddress();
                int portIn = packet.getPort();
                byte[] data = packet.getData();
                byte messageTypeByte = data[0];
                byte idempotentTypeByte = data[1];
                byte[] requestIdBytesIn = new byte[2];
                System.arraycopy(data, 2, requestIdBytesIn, 0, 2);
                int requestIdIn = getBytesAsHalfWord(requestIdBytesIn);
                MSGTYPE messageType = getMessageType(messageTypeByte, idempotentTypeByte);
                boolean isResponse = (messageType == MSGTYPE.IDEMPOTENT_RESPONSE || messageType == MSGTYPE.NON_IDEMPOTENT_RESPONSE);
                if (isResponse && requestIdIn == requestIdOut) {
                    byte[] inBody = Arrays.copyOfRange(data, 4, payload.length);
                    return  inBody;
                } else {
                    handlePacketIn(data, addressIn, portIn);
                }
            } catch (SocketTimeoutException ste) {
                sendRequestPacketOut(payload, address, port);
            }
        } while(resend);
        return null;
    }

    public void addObjectReference(String name, RemoteObject objRef){
        this.binder.addObjectReference(name, objRef);
    }

    public void setBinder(Binder binder){
        this.binder = binder;
    }

}