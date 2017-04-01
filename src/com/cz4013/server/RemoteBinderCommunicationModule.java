package com.cz4013.server;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.*;

/**
 * Created by danielseetoh on 4/1/17.
 */
public class RemoteBinderCommunicationModule extends Thread{
    protected boolean isRunning = true;
    protected DatagramSocket socket = null;
    protected InetAddress serverAddress;
    protected int port;
    protected InetAddress remoteBinderAddress;
    protected int remoteBinderPort;
    private final int MAX_BYTE_SIZE = 1024;
    private volatile boolean exit = false;

    public RemoteBinderCommunicationModule(int serverPort, String remoteBinderAddress, int remoteBinderPort) throws IOException{
        super("RemoteBinderCommunicationModule");
        socket = new DatagramSocket(new InetSocketAddress(serverPort));
        this.remoteBinderAddress = InetAddress.getByName(remoteBinderAddress);
        this.remoteBinderPort = remoteBinderPort;
    }

    public void run() {
        System.out.println("RemoteBinderCommunicationModule is running.");
        while (!exit){
        }
        System.out.println("RemoteBinderCommunicationModule is stopping.");
        try {
            socket.setReuseAddress(true);
        } catch (SocketException e){
            e.printStackTrace();
        }
        socket.close();
    }

    /**
     * creates an add request string to send over to the remote binder
     * @param remoteObjectName
     * @return
     * @throws IOException
     */
    public String sendAddRequest(String remoteObjectName, String remoteObjectReference) throws IOException{
        String request = "ADD";
        request += " " + remoteObjectName + " " + remoteObjectReference;
        byte[] payload = new byte[MAX_BYTE_SIZE];
        byte[] requestBytes = request.getBytes();
        System.arraycopy(requestBytes, 0, payload, 0, requestBytes.length);
        return sendRequestPacketOut(payload, this.remoteBinderAddress, this.remoteBinderPort);
    }

    /**
     * creates a get request string to send over to the remote binder
     * @param remoteObjectName
     * @return
     * @throws IOException
     */
    public String sendGetRequest(String remoteObjectName) throws IOException{
        String request = "GET";
        request += " " + remoteObjectName;
        byte[] payload = new byte[MAX_BYTE_SIZE];
        byte[] requestBytes = request.getBytes();
        System.arraycopy(requestBytes, 0, payload, 0, requestBytes.length);
        return sendRequestPacketOut(payload, this.remoteBinderAddress, this.remoteBinderPort);
    }

    /**
     * Sends request packet to remote binder address repeatedly until it gets a response
     * @param payload
     * @param address
     * @param port
     * @return
     */
    public String sendRequestPacketOut(byte[] payload, InetAddress address, int port) throws IOException{
        if (payload == null){
            return null;
        }
        boolean resend = true;
        int counter = 0;
        int failCounter = 0;
        do{
            byte[] outBuf = payload;
            DatagramPacket packet = new DatagramPacket(outBuf, outBuf.length, address, port);
            socket.send(packet);
            byte[] bufIn = new byte[MAX_BYTE_SIZE];
            packet = new DatagramPacket(bufIn, bufIn.length);
            socket.receive(packet);
            byte[] data = packet.getData();
            String dataString = new String(data);
            if (dataString.contains("Success")){
                return dataString.trim();
            } else {
                failCounter += 1;
                System.out.println(dataString.trim());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
                if (failCounter > 5){
                    System.out.println("Please try again next time.");
                    break;
                }
            }
            counter += 1;
            if (counter > 5){
                System.out.println("Please check if there are any rectifiable issues.");
                break;
            }
        } while(resend);
        return null;
    }

    public void setExit(boolean exit){
        this.exit = exit;
    }


}
