/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.client.builder;

import io.undertow.client.ClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class CacheableConnection {

    private static Logger logger = LoggerFactory.getLogger(CacheableConnection.class);

    private ClientConnection clientConnection;
    private long ttl;
    private long lifeStartTime;
    //private long ttlParked = 100; // for the test case ConnectionCacheManager only.
    private long ttlParked = 60*1000; // default to 1 minutes to closed the parked connection.
    private long lifeStartTimeParked;
    private int maxReqCount=-1;
    private int requestCount;

    /**
     * Constructor to create a CacheableConnection
     * @param clientConnection Undertow ClientConnection
     * @param ttl ttl that is used to move the HTTP2 connection to parked after expired
     * @param maxReqCount max request count to move the HTTP2 connection to parked
     */
    public CacheableConnection(ClientConnection clientConnection, long ttl, int maxReqCount) {
        this.clientConnection = clientConnection;
        this.ttl = ttl;
        this.lifeStartTime = System.currentTimeMillis();
        this.maxReqCount= maxReqCount;
    }

    /**
     * Constructor to create a CacheableConnection
     * @param clientConnection Undertow ClientConnection
     * @param ttl ttl that is used to move the HTTP2 connection to parked after expired
     * @param maxReqCount max request count to move the HTTP2 connection to parked
     * @param ttlParked ttl that the parked connection will be closed.
     */
    public CacheableConnection(ClientConnection clientConnection, long ttl, int maxReqCount, long ttlParked) {
        this(clientConnection, ttl, maxReqCount);
        ttlParked = ttlParked;
    }

    public boolean isOpen() {
        if((System.currentTimeMillis() > this.lifeStartTime + ttl) || (requestCount >= maxReqCount && maxReqCount != -1)) {
            logger.debug("Connection expired. Start time of this connection is {}. The total request count is {}", new Date(this.lifeStartTime), this.requestCount);
            this.lifeStartTimeParked = System.currentTimeMillis(); // Start the life time of the parked connection
            this.requestCount = 0;
            return false;
        }
        ++requestCount;
        return this.clientConnection.isOpen();
    }

    public boolean isParkedConnectionExpired() {
        if(System.currentTimeMillis() > (this.lifeStartTimeParked + this.ttlParked)) {
            logger.info("ParkedConnection expired. Start time of this parked connection is {}", new Date(this.lifeStartTimeParked));
            try {
                this.clientConnection.close();
            } catch (Exception ignored){
                logger.info("Exception while closing the parked connection. This exception is suppressed. Exception is {}", ignored);
            }
            return true;
        }
        return false;
    }

    public ClientConnection getCachedConnection() {
        return this.clientConnection;
    }
}

