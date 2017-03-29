package com.cz4013.server;

import java.net.InetAddress;

/**
 * Created by melvynsng on 3/29/17.
 */
public class Monitor {
    public InetAddress address;
    int port;
    long expiry;

    public Monitor (InetAddress address, int port, long expiry) {
        this.address = address;
        this.port = port;
        this.expiry = expiry;
    }
}
