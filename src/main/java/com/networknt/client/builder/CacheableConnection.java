package com.networknt.client.builder;

import io.undertow.client.ClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheableConnection {

    private static Logger logger = LoggerFactory.getLogger(CacheableConnection.class);

    private ClientConnection clientConnection;
    private long ttl;
    private long lifeStartTime;

    public CacheableConnection(ClientConnection clientConnection, long ttl) {
        this.clientConnection = clientConnection;
        this.ttl = ttl;
        this.lifeStartTime = System.currentTimeMillis();
    }

    public boolean isOpen() {
        if(System.currentTimeMillis() > this.lifeStartTime + ttl) {
            logger.debug("Connection expired.");
            try {
                this.clientConnection.close();
            } catch (Exception ignored){ }
            return false;
        }
        return this.clientConnection.isOpen();
    }

    public ClientConnection getCachedConnection() {
        return this.clientConnection;
    }
}
