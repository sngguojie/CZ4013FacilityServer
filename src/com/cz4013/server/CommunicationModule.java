package com.cz4013.server;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class CommunicationModule extends Thread {
    protected boolean isRunning = true;
    protected DatagramSocket socket = null;
    protected HashMap<byte[], byte[]> messageHistory = new HashMap<byte[], byte[]>();
    protected HashMap<byte[], Boolean> receivedRequest = new HashMap<byte[], Boolean>();
    protected enum MSGTYPE {IDEMPOTENT_REQUEST, NON_IDEMPOTENT_REQUEST, IDEMPOTENT_RESPONSE, NON_IDEMPOTENT_RESPONSE};
    protected enum DATATYPE {STRING, INT};
    protected InetAddress serverAddress;
    protected int serverPort;
    protected HashMap<Integer, byte[]> requestHistory = new HashMap<Integer,byte[]>();
    protected HashMap<Integer, Boolean> receivedResponse = new HashMap<Integer,Boolean>();
    Random random = new Random();
    private Binder binder;
    private final int MAX_BYTE_SIZE = 1024;
    private boolean atLeastOne;
    private boolean printMessageHeadOn = false;
    private float lossRate;

    public CommunicationModule(boolean atLeastOne) throws IOException {
        // PORT 2222 is default for NTU computers
        this("CommunicationModule", 2222, atLeastOne);
    }

    public CommunicationModule(String name, int PORT, boolean atLeastOne) throws IOException {
        super(name);
        socket = new DatagramSocket(new InetSocketAddress(PORT));
//        serverPort = PORT;
        String[] localHostString = InetAddress.getLocalHost().toString().split("/");
        System.out.println(localHostString[localHostString.length - 1]);
        serverAddress = InetAddress.getByName(localHostString[localHostString.length - 1]);
        this.atLeastOne = atLeastOne;
    }

    public void waitForPacket () {

        while (this.isRunning) {
            try {
                byte[] buf = new byte[MAX_BYTE_SIZE];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                if (isPacketLoss()) {
                    continue;
                }
                printMessageHead(packet,true);
                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                handlePacketIn(packet.getData(), address, port);
            } catch (IOException e) {
                e.printStackTrace();
                isRunning = false;
            }
        }
    }

    public void setLossRate (float lossRate) {
        this.lossRate = lossRate;
    }


    public void run () {
        System.out.println("CommunicationModule Running");
        waitForPacket();
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
        return ((bytes[1] & 0xff) << 8) | (bytes[0] & 0xff);
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

    private RemoteObject getRemoteObject (byte[] payload) {
        String objectRefName = MarshalModule.unmarshal(payload).getObjectReference();
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
                System.out.println("this.atLeastOnce" + this.atLeastOne);
                System.out.println("messageHistory.containsKey(inHead)" + messageHistory.containsKey(inHead));
                if (!this.atLeastOne && messageHistory.containsKey(inHead)) {
                    out = messageHistory.get(inHead);
                    sendReponsePacketOut(out, address, port);
                    System.out.println("get message from messageHistory");
                    break;
                }
                if (!this.atLeastOne && receivedRequest.containsKey(inHead)) {
                    break;
                }
                receivedRequest.put(inHead, true);
                outHead = getResponseHead(MSGTYPE.IDEMPOTENT_RESPONSE, requestId);
                outBody = getRemoteObjectResponse(inBody);
                out = combineByteArrays(outHead, outBody);
                messageHistory.put(inHead,out);
                System.out.println("store message in messageHistory");
                sendReponsePacketOut(out, address, port);
                break;
            case NON_IDEMPOTENT_REQUEST:
                System.out.println("this.atLeastOnce" + this.atLeastOne);
                System.out.println("messageHistory.containsKey(inHead)" + messageHistory.containsKey(inHead));
                if (!this.atLeastOne && messageHistory.containsKey(inHead)) {
                    out = messageHistory.get(inHead);
                    sendReponsePacketOut(out, address, port);
                    System.out.println("get message from messageHistory");
                    break;
                }
                if (!this.atLeastOne && receivedRequest.containsKey(inHead)) {
                    break;
                }
                receivedRequest.put(inHead, true);

                outHead = getResponseHead(MSGTYPE.IDEMPOTENT_RESPONSE, requestId);
                outBody = getRemoteObjectResponse(inBody);
                out = combineByteArrays(outHead, outBody);
                messageHistory.put(inHead,out);
                System.out.println("store message in messageHistory");
                sendReponsePacketOut(out, address, port);
                break;
            case IDEMPOTENT_RESPONSE:
                if (receivedResponse.containsKey(getRequestId(inHead))) {
                    receivedResponse.put(getRequestId(inHead), true);
                }
                break;
            case NON_IDEMPOTENT_RESPONSE:
                if (receivedResponse.containsKey(getRequestId(inHead))) {
                    receivedResponse.put(getRequestId(inHead), true);
                }
                break;
            default:
                break;
        }

    }

    private byte[] getRemoteObjectResponse (byte[] requestBody) {
        RemoteObject remoteObject = getRemoteObject(requestBody);
        return remoteObject.handleRequest(requestBody);
    }

    private int getNewRequestId () {
        int i = 0;
        while (requestHistory.containsKey(i)) {
            i++;
        }
        if (i > Short.MAX_VALUE) {
            i = 0;
        }
        return i;
    }

    public byte[] sendRequest(boolean isIdempotent, byte[] data) {
        return sendRequest(isIdempotent, data, serverAddress, serverPort);
    }

    public void sendResponse(boolean isIdempotent, byte[] data) {
        sendResponse(isIdempotent, data, serverAddress, serverPort);
    }

    public byte[] sendRequest(boolean isIdempotent, byte[] data, InetAddress address, int port) {
        try {
            byte[] payload = makePayload(isIdempotent, true, data);
            return sendRequestPacketOut(payload, address, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void sendResponse(boolean isIdempotent, byte[] data, InetAddress address, int port) {
        try {
            byte[] payload = makePayload(isIdempotent, false, data);
            sendReponsePacketOut(payload, address, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] makePayload (boolean isIdempotent, boolean isRequest, byte[] data) throws IOException {
        int requestIdInt = getNewRequestId();
        MSGTYPE messageType;
        if (isRequest)
            messageType = isIdempotent ? MSGTYPE.IDEMPOTENT_REQUEST : MSGTYPE.NON_IDEMPOTENT_REQUEST;
        else
            messageType = isIdempotent ? MSGTYPE.IDEMPOTENT_RESPONSE : MSGTYPE.NON_IDEMPOTENT_RESPONSE;
        byte[] outHead = getResponseHead(messageType, requestIdInt);
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
        printMessageHead(packet,false);
        if (isPacketLoss()) {
            return;
        }
        socket.send(packet);
    }

    private byte[] sendRequestPacketOut (byte[] payload, InetAddress address, int port) throws IOException {
        if (payload == null) {
            return null;
        }
        boolean resend = true;
        byte[] requestIdBytesOut = new byte[2];
        System.arraycopy(payload, 2, requestIdBytesOut, 0, 2);
        int requestIdOut = getBytesAsHalfWord(requestIdBytesOut);
        // send request
        do {
            try {
                byte[] buf = payload;
                DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
                printMessageHead(packet,false);
                receivedResponse.put(requestIdOut, false);
                new TimerThread(this, socket, packet, requestIdOut, 500l).start();
                byte[] bufIn = new byte[MAX_BYTE_SIZE];
                packet = new DatagramPacket(bufIn, bufIn.length);
                socket.receive(packet);

                printMessageHead(packet,true);
                InetAddress addressIn = packet.getAddress();
                int portIn = packet.getPort();
                byte[] data = packet.getData();
                if (isResponse(data) && getRequestId(data) == requestIdOut) {
                    receivedResponse.put(requestIdOut, true);
                    byte[] inBody = Arrays.copyOfRange(data, 4, payload.length);
                    return inBody;
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

    public void setWaitingForPacket(boolean wait){
        this.isRunning = wait;
    }

    private MSGTYPE getMessageType (byte[] payload) {
        return getMessageType(payload[0], payload[1]);
    }

    private boolean isResponse(byte[] payload) {
        MSGTYPE messageType = getMessageType(payload[0], payload[1]);
        return (messageType == MSGTYPE.NON_IDEMPOTENT_RESPONSE || messageType == MSGTYPE.IDEMPOTENT_RESPONSE);
    }

    private int getRequestId (byte[] payload) {
        return getBytesAsHalfWord(Arrays.copyOfRange(payload, 2, 4));
    }

    private void printMessageHead (DatagramPacket packet, boolean isIncoming) {
        if (this.printMessageHeadOn) {
            String arrow = isIncoming ? " IN  " : " OUT ";
            System.out.println(arrow + messageHeadString(packet));
        }
    }

    public void setPrintMessageHead (boolean on) {
        this.printMessageHeadOn = on;
    }

    public void printMessageHistory (DatagramPacket packet, boolean isExecuted) {
        if (this.printMessageHeadOn) {
            String retrieve = isExecuted ? " STR_MSG   " : " RTRV_MSG ";
            System.out.println(retrieve + messageHeadString(packet));
        }
    }

    private String messageHeadString (DatagramPacket packet) {
        String ipAddress = packet.getAddress().toString();
        int port = packet.getPort();
        byte[] payload = packet.getData();
        MSGTYPE messageType = getMessageType(payload);
        int requestId = getRequestId(payload);
        return "t: " + (System.currentTimeMillis() % 600000l) + " " + messageType.toString() + " " + ipAddress + ":" + port +" requestID {" + requestId + "} ";
    }

    public boolean gotResponse(int requestId) {
        return receivedResponse.containsKey(requestId) && receivedResponse.get(requestId);
    }

    public boolean isPacketLoss() {
        return random.nextFloat() <= this.lossRate;
    }
}