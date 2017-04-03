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
    protected HashMap<String, byte[]> messageHistory = new HashMap<String, byte[]>();
    protected HashMap<String, Boolean> receivedRequest = new HashMap<String, Boolean>();
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
        String[] localHostString = InetAddress.getLocalHost().toString().split("/");
        System.out.println(localHostString[localHostString.length - 1]);
        serverAddress = InetAddress.getByName(localHostString[localHostString.length - 1]);
        this.atLeastOne = atLeastOne;
    }

    /**
     * For listening for incoming packets while the thread is running
     */
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

    /**
     * To set the loss rate for packets (probability that a packet is loss during sending/receiveing)
     * @param lossRate
     */
    public void setLossRate (float lossRate) {
        this.lossRate = lossRate;
    }

    /**
     * To start listening for requests when the thread starts
     */
    public void run () {
        System.out.println("CommunicationModule Running");
        waitForPacket();
        socket.close();
    }

    /**
     * To identify the message type of the packet given
     * @param messageTypeByte
     * @param idempotentTypeByte
     * @return
     */
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

    /**
     * Returns the byte array for a speified message type
     * @param messageType
     * @return
     */
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

    /**
     * To construct a response head to be appended to the packet given a message type and request id
     * @param messageType
     * @param requestId
     * @return
     */
    private byte[] getResponseHead (MSGTYPE messageType, int requestId) {
        byte[] requestIdBytes = ByteUtils.getHalfWordAsBytes(requestId);
        byte[] messageTypeBytes = getMessageTypeAsBytes(messageType);
        byte[] result = new byte[4];
        System.arraycopy(messageTypeBytes, 0, result, 0, 2);
        System.arraycopy(requestIdBytes, 0, result, 2, 2);
        return result;
    }

    /**
     * To identify and retrieve the Remote Object from the payload data of the packet
     * @param payload
     * @return
     */
    private RemoteObject getRemoteObject (byte[] payload) {
        String objectRefName = MarshalModule.unmarshal(payload).getObjectReference();
        return binder.getObjectReference(objectRefName);
    }

    /**
     * To handle an incoming packet by deciding what actions to execute based on the contents of the packet data
     * @param payload
     * @param address
     * @param port
     * @throws IOException
     */
    private void handlePacketIn(byte[] payload, InetAddress address, int port) throws IOException {
        byte messageTypeByte = payload[0];
        byte idempotentTypeByte = payload[1];
        byte[] requestIdBytes = new byte[2];
        System.arraycopy(payload, 2, requestIdBytes, 0, 2);

        MSGTYPE messageType = getMessageType(messageTypeByte, idempotentTypeByte);
        int requestId = ByteUtils.getBytesAsHalfWord(requestIdBytes);
        byte[] inHead = Arrays.copyOfRange(payload, 0, 4);
        byte[] inBody = Arrays.copyOfRange(payload, 4, payload.length);
        byte[] outHead, outBody, out;
        String clientRequestString = getClientRequestString(requestId, address.toString(), port);

        switch (messageType) {
            case IDEMPOTENT_REQUEST:
                System.out.println("this.atLeastOnce" + this.atLeastOne);
                System.out.println("messageHistory.containsKey(clientRequestString)" + messageHistory.containsKey(clientRequestString));
                if (!this.atLeastOne && messageHistory.containsKey(clientRequestString)) {
                    out = messageHistory.get(clientRequestString);
                    sendReponsePacketOut(out, address, port);
                    System.out.println("get message from messageHistory");
                    break;
                }
                if (!this.atLeastOne && receivedRequest.containsKey(clientRequestString)) {
                    break;
                }
                receivedRequest.put(clientRequestString, true);
                outHead = getResponseHead(MSGTYPE.IDEMPOTENT_RESPONSE, requestId);
                outBody = getRemoteObjectResponse(inBody);
                out = ByteUtils.combineByteArrays(outHead, outBody);
                messageHistory.put(clientRequestString,out);
                System.out.println("store message in messageHistory");
                sendReponsePacketOut(out, address, port);
                break;
            case NON_IDEMPOTENT_REQUEST:
                System.out.println("this.atLeastOnce" + this.atLeastOne);
                System.out.println("messageHistory.containsKey(inHead)" + messageHistory.containsKey(clientRequestString));
                if (!this.atLeastOne && messageHistory.containsKey(clientRequestString)) {
                    out = messageHistory.get(clientRequestString);
                    sendReponsePacketOut(out, address, port);
                    System.out.println("get message from messageHistory");
                    break;
                }
                if (!this.atLeastOne && receivedRequest.containsKey(clientRequestString)) {
                    break;
                }
                receivedRequest.put(clientRequestString, true);

                outHead = getResponseHead(MSGTYPE.IDEMPOTENT_RESPONSE, requestId);
                outBody = getRemoteObjectResponse(inBody);
                out = ByteUtils.combineByteArrays(outHead, outBody);
                messageHistory.put(clientRequestString,out);
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

    /**
     * Returns a unique string for each client request
     * @param requestID
     * @param address
     * @param port
     * @return
     */
    private String getClientRequestString (int requestID, String address, int port) {
        return address +" "+Integer.toString(port)+" "+Integer.toString(requestID);
    }

    /**
     * To get the response from a remote object given the byte array data from the packet
     * Returns the repsonse of the remote object in the form of a byte array
     * @param requestBody
     * @return
     */
    private byte[] getRemoteObjectResponse (byte[] requestBody) {
        RemoteObject remoteObject = getRemoteObject(requestBody);
        return remoteObject.handleRequest(requestBody);
    }

    /**
     * To generate a new request ID for each new request initiated by the server
     * @return
     */
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

    /**
     * To send a request to the server
     * @param isIdempotent
     * @param data
     * @return
     */
    public byte[] sendRequestToServer(boolean isIdempotent, byte[] data) {
        return sendRequest(isIdempotent, data, serverAddress, serverPort);
    }

    /**
     * To send a response to the server
     * @param isIdempotent
     * @param data
     */
    public void sendResponseToServer(boolean isIdempotent, byte[] data) {
        sendResponse(isIdempotent, data, serverAddress, serverPort);
    }

    /**
     * To send a request to a specified address and port
     * @param isIdempotent
     * @param data
     * @param address
     * @param port
     * @return
     */
    public byte[] sendRequest(boolean isIdempotent, byte[] data, InetAddress address, int port) {
        try {
            byte[] payload = makePayload(isIdempotent, true, data);
            return sendRequestPacketOut(payload, address, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * To send a response to a specified address and port
     * @param isIdempotent
     * @param data
     * @param address
     * @param port
     */
    public void sendResponse(boolean isIdempotent, byte[] data, InetAddress address, int port) {
        try {
            byte[] payload = makePayload(isIdempotent, false, data);
            sendReponsePacketOut(payload, address, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * To construct the payload for a packet given the marshalled byte array data and the message type
     * @param isIdempotent
     * @param isRequest
     * @param data
     * @return
     * @throws IOException
     */
    private byte[] makePayload (boolean isIdempotent, boolean isRequest, byte[] data) throws IOException {
        int requestIdInt = getNewRequestId();
        MSGTYPE messageType;
        if (isRequest)
            messageType = isIdempotent ? MSGTYPE.IDEMPOTENT_REQUEST : MSGTYPE.NON_IDEMPOTENT_REQUEST;
        else
            messageType = isIdempotent ? MSGTYPE.IDEMPOTENT_RESPONSE : MSGTYPE.NON_IDEMPOTENT_RESPONSE;
        byte[] outHead = getResponseHead(messageType, requestIdInt);
        byte[] out = ByteUtils.combineByteArrays(outHead, data);
        requestHistory.put(requestIdInt,out);
        return out;
    }

    /**
     * To send a response packet to the specified address and port
     * @param payload
     * @param address
     * @param port
     * @throws IOException
     */
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

    /**
     * To send a request packed out to the specified address and port
     * The difference with the response packet is the request implements a timeout which will resend the request if no response has been received
     * @param payload
     * @param address
     * @param port
     * @return
     * @throws IOException
     */
    private byte[] sendRequestPacketOut (byte[] payload, InetAddress address, int port) throws IOException {
        if (payload == null) {
            return null;
        }
        boolean resend = true;
        byte[] requestIdBytesOut = new byte[2];
        System.arraycopy(payload, 2, requestIdBytesOut, 0, 2);
        int requestIdOut = ByteUtils.getBytesAsHalfWord(requestIdBytesOut);
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

    /**
     * To add an object reference to the binder
     * @param name
     * @param objRef
     */
    public void addObjectReference(String name, RemoteObject objRef){
        this.binder.addObjectReference(name, objRef);
    }

    /**
     * To set the reference to the binder
     * @param binder
     */
    public void setBinder(Binder binder){
        this.binder = binder;
    }

    /**
     * To set whether to wait for an incoming packet
     * @param wait
     */
    public void setWaitingForPacket(boolean wait){
        this.isRunning = wait;
    }

    /**
     * To get the message type given the byte array payload from a packet
     * @param payload
     * @return
     */
    private MSGTYPE getMessageType (byte[] payload) {
        return getMessageType(payload[0], payload[1]);
    }

    /**
     * To check if the payload is of message type response
     * @param payload
     * @return
     */
    private boolean isResponse(byte[] payload) {
        MSGTYPE messageType = getMessageType(payload[0], payload[1]);
        return (messageType == MSGTYPE.NON_IDEMPOTENT_RESPONSE || messageType == MSGTYPE.IDEMPOTENT_RESPONSE);
    }

    /**
     * To get the integer request id from the payload
     * @param payload
     * @return
     */
    private int getRequestId (byte[] payload) {
        return ByteUtils.getBytesAsHalfWord(Arrays.copyOfRange(payload, 2, 4));
    }

    /**
     * To print the message head information onto the console
     * @param packet
     * @param isIncoming
     */
    private void printMessageHead (DatagramPacket packet, boolean isIncoming) {
        if (this.printMessageHeadOn) {
            String arrow = isIncoming ? " IN  " : " OUT ";
            System.out.println(arrow + messageHeadString(packet));
        }
    }

    /**
     * To set whether to show the message header information on the console
     * @param on
     */
    public void setPrintMessageHead (boolean on) {
        this.printMessageHeadOn = on;
    }

    /**
     * To print whether a message is executed
     * @param packet
     * @param isExecuted
     */
    public void printMessageHistory (DatagramPacket packet, boolean isExecuted) {
        if (this.printMessageHeadOn) {
            String retrieve = isExecuted ? " STR_MSG   " : " RTRV_MSG ";
            System.out.println(retrieve + messageHeadString(packet));
        }
    }

    /**
     * To retuen the message header string for printing
     * @param packet
     * @return
     */
    private String messageHeadString (DatagramPacket packet) {
        String ipAddress = packet.getAddress().toString();
        int port = packet.getPort();
        byte[] payload = packet.getData();
        MSGTYPE messageType = getMessageType(payload);
        int requestId = getRequestId(payload);
        return "t: " + (System.currentTimeMillis() % 600000l) + " " + messageType.toString() + " " + ipAddress + ":" + port +" requestID {" + requestId + "} ";
    }

    /**
     * Returns true if the response has been received for the request ID specified
     * @param requestId
     * @return
     */
    public boolean gotResponse(int requestId) {
        return receivedResponse.containsKey(requestId) && receivedResponse.get(requestId);
    }

    /**
     * Returns true if the packet is simulated to be loss based on a random function and predefined loss rate
     * @return
     */
    public boolean isPacketLoss() {
        return random.nextFloat() <= this.lossRate;
    }
}