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

import com.networknt.client.model.ServiceDef;
import com.networknt.client.model.TimeoutDef;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.HttpString;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class HttpClientRequest implements AutoCloseable {
    private ServiceDef serviceDef;
    private Boolean isHttp2Enabled = true;
    private ClientRequest clientRequest;
    private String requestBody;
    private Boolean addCCToken = false;
    private String authToken;
    private Boolean propagateHeaders = false;
    private CountDownLatch latch;
    private TimeoutDef connectionRequestTimeout = new TimeoutDef(5, TimeUnit.SECONDS);
    private TimeoutDef requestTimeout = new TimeoutDef(5, TimeUnit.SECONDS);
    private long connectionCacheTTLms = 10000;
    private AtomicReference<ClientResponse> responseReference;

    private String apiHost;

    private int maxReqCount=-1;
    private long parkedConnectionTTL;


    // Cached thread pool as we might expect many short-lived threads..
    private ExecutorService executorService = Executors.newCachedThreadPool();

    Future<ClientResponse> triggerLatchAwait() {
        return executorService.submit(() -> {
            try {
                if (requestTimeout != null) {
                    latch.await(requestTimeout.getTimeout(), requestTimeout.getUnit());
                } else {
                    latch.await();
                }
                return responseReference.get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public ServiceDef getServiceDef() {
        return serviceDef;
    }

    public void setServiceDef(ServiceDef serviceDef) {
        this.serviceDef = serviceDef;
    }

    public Boolean getHttp2Enabled() {
        return isHttp2Enabled;
    }

    public void setHttp2Enabled(Boolean http2Enabled) {
        isHttp2Enabled = http2Enabled;
    }

    public ClientRequest getClientRequest() {
        return clientRequest;
    }

    public void setClientRequest(ClientRequest clientRequest) {
        this.clientRequest = clientRequest;
    }

    public Boolean getAddCCToken() {
        return addCCToken;
    }

    public void setAddCCToken(Boolean addCCToken) {
        this.addCCToken = addCCToken;
    }

    public Boolean getPropagateHeaders() {
        return propagateHeaders;
    }

    public void setPropagateHeaders(Boolean propagateHeaders) {
        this.propagateHeaders = propagateHeaders;
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }

    public TimeoutDef getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(TimeoutDef requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public TimeoutDef getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    public void setConnectionRequestTimeout(TimeoutDef connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    public long getConnectionCacheTTLms() {
        return connectionCacheTTLms;
    }

    public void setConnectionCacheTTLms(long connectionCacheTTLms) {
        this.connectionCacheTTLms = connectionCacheTTLms;
    }

    public AtomicReference<ClientResponse> getResponseReference() {
        return responseReference;
    }

    public void setResponseReference(AtomicReference<ClientResponse> responseReference) {
        this.responseReference = responseReference;
    }

    @Override
    public void close() throws Exception {
        executorService.shutdown();
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public void setHeaderValue(HttpString headerName, String headerValue) {
        getClientRequest().getRequestHeaders().put(headerName, headerValue);
    }

    public void setHeaderMap(Map<String, ?> headMap) {
        if (headMap!=null) {
            headMap.forEach((k,v)->getClientRequest().getRequestHeaders().put(new HttpString(k), v.toString()));
        }
     }

    public String getApiHost() {
        return apiHost;
    }

    public void setApiHost(String apiHost) {
        this.apiHost = apiHost;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
    public int getMaxReqCount() {
        return maxReqCount;
    }

    public void setMaxReqCount(int maxReqCount) {
        this.maxReqCount = maxReqCount;
    }

    public long getParkedConnectionTTL() {
        return parkedConnectionTTL;
    }

    public void setParkedConnectionTTL(long parkedConnectionTTL) {
        this.parkedConnectionTTL = parkedConnectionTTL;
    }
}
