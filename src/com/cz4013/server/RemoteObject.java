package com.cz4013.server;

import java.net.InetAddress;

/**
 * Created by melvynsng on 3/30/17.
 */
public interface RemoteObject {
    public byte[] handleRequest (byte[] requestBody, InetAddress address, int port);
}
