package com.cz4013.server;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by melvynsng on 3/30/17.
 */
public interface RemoteObject {
    public byte[] handleRequest (byte[] requestBody);
}
