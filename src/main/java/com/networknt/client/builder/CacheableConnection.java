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

public class CacheableConnection {

    private static Logger logger = LoggerFactory.getLogger(CacheableConnection.class);

    private ClientConnection clientConnection;
    private long ttl;
    private long lifeStartTime;
    private int maxReqCount=-1;
    private int requestCount;

    public CacheableConnection(ClientConnection clientConnection, long ttl, int maxReqCount) {
        this.clientConnection = clientConnection;
        this.ttl = ttl;
        this.lifeStartTime = System.currentTimeMillis();
        this.maxReqCount= maxReqCount;
    }

    public boolean isOpen() {
        if(System.currentTimeMillis() > this.lifeStartTime + ttl||(requestCount>=maxReqCount && maxReqCount!=-1)) {
            logger.debug("Connection expired.");
            try {
                this.clientConnection.close();
                requestCount=0;
            } catch (Exception ignored){ }
            return false;
        }
        ++requestCount;
        return this.clientConnection.isOpen();
    }

    public ClientConnection getCachedConnection() {
        return this.clientConnection;
    }
}

