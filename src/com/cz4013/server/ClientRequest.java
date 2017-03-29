package com.cz4013.server;

import java.net.InetAddress;

/**
 * Created by melvynsng on 3/30/17.
 */
public class ClientRequest {
    InetAddress address;
    int port;
    byte requestId;
    public ClientRequest (InetAddress address, int port, byte requestId) {
        this.address = address;
        this.port = port;
        this.requestId = requestId;
    }

    public boolean equals(ClientRequest other) {
        return  (this.address == other.address) && (this.port == other.port) && (this.requestId == other.requestId);
    }
}
