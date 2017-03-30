package com.cz4013.server;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by melvynsng on 3/30/17.
 */
public class CommunicationModule extends Thread {
    protected boolean isRunning = true;
    protected DatagramSocket socket = null;
    protected HashMap<String, RemoteObject> objectReference = new HashMap<String, RemoteObject>();
    protected HashMap<byte[], byte[]> messageHistory = new HashMap<byte[], byte[]>();
    protected enum MSGTYPE {IDEMPOTENT_REQUEST, NON_IDEMPOTENT_REQUEST, IDEMPOTENT_RESPONSE, NON_IDEMPOTENT_RESPONSE};
    protected enum DATATYPE {STRING, INT};
    protected InetAddress serverAddress;
    protected int serverPort;
    protected HashMap<Integer, byte[]> requestHistory = new HashMap<Integer,byte[]>();

    public CommunicationModule() throws IOException {
        // PORT 2222 is default for NTU computers
        this("CommunicationModule", 2222);
    }

    public CommunicationModule(String name, int PORT) throws IOException {
        super(name);
        socket = new DatagramSocket(new InetSocketAddress(PORT));
        serverPort = PORT;
        String[] localHostString = InetAddress.getLocalHost().toString().split("/");
        System.out.println(localHostString[localHostString.length - 1]);
        serverAddress = InetAddress.getByName(localHostString[localHostString.length - 1]);
    }

    public void run () {
        System.out.println("CommunicationModule Running");
        while (this.isRunning) {
            try {
                byte[] buf = new byte[256];
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
        DATATYPE dataType = getDataType(Arrays.copyOfRange(payload, 0, 4));
        if (dataType != DATATYPE.STRING) {
            return null;
        }
        int stringLen = ByteBuffer.wrap(Arrays.copyOfRange(payload, 4, 8)).getInt();
        stringLen += 4 - (stringLen % 4);
        String objectRefName = Arrays.copyOfRange(payload, 8, 8 + stringLen).toString();
        return objectReference.get(objectRefName);
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
                sendPacketOut(out, address, port);
                break;
            case NON_IDEMPOTENT_REQUEST:
                if (messageHistory.containsKey(inHead)) {
                    out = messageHistory.get(inHead);
                    sendPacketOut(out, address, port);
                    break;
                }
                outHead = getResponseHead(MSGTYPE.IDEMPOTENT_RESPONSE, requestId);
                outBody = getRemoteObjectResponse(inBody);
                out = combineByteArrays(outHead, outBody);
                messageHistory.put(inHead,out);
                sendPacketOut(out, address, port);
                break;
            case IDEMPOTENT_RESPONSE:
                getRemoteObjectResponse(inBody);
                break;
            case NON_IDEMPOTENT_RESPONSE:
                getRemoteObjectResponse(inBody);
                break;
            default:
                break;
        }

    }

    private byte[] getRemoteObjectResponse (byte[] requestBody) {
        RemoteObject remoteObject = getRemoteObject(requestBody);
        return remoteObject.handleRequest(Arrays.copyOfRange(requestBody,1,requestBody.length));
    }

    private int getNewRequestId () {
        int i = 0;
        while (requestHistory.containsKey(i)) {
            i++;
        }
        return i;
    }

    public void sendPayload (byte[] data) throws IOException {
        sendPayload(data, serverAddress, serverPort);
    }

    public void sendPayload (byte[] data, InetAddress address, int port) throws IOException {
        int requestIdInt = getNewRequestId();
        byte[] outHead = getResponseHead(MSGTYPE.IDEMPOTENT_REQUEST, requestIdInt);
        byte[] out = combineByteArrays(outHead, data);
        requestHistory.put(requestIdInt,out);
        sendPacketOut(out, address, port);
    }

    private void sendPacketOut (byte[] payload, InetAddress address, int port) throws IOException {
        if (payload == null) {
            return;
        }

        // send request
        try {
            byte[] buf = payload;
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
            socket.setSoTimeout(5000);
            socket.send(packet);
        } catch (SocketTimeoutException ste) {
            sendPacketOut(payload, address, port);
        }
    }

}