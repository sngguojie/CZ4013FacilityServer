package com.cz4013.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Created by melvynsng on 4/1/17.
 */
public class TimerThread extends Thread {

    CommunicationModule cm;
    DatagramSocket socket;
    DatagramPacket packet;
    long timeout;
    int requestId;

    public TimerThread (CommunicationModule cm, DatagramSocket socket, DatagramPacket packet, int requestId, long timeout) {
        this.cm = cm;
        this.socket = socket;
        this.packet = packet;
        this.timeout = timeout;
        this.requestId = requestId;
    }

    public void run () {
        do {
            System.out.println("Sending Packet");
            try {
                if (!cm.isPacketLoss()) {
                    socket.send(packet);
                }
                Thread.sleep(this.timeout);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        } while (needResend());
    }

    private boolean needResend() {
        return !cm.gotResponse(this.requestId);
    }

}
