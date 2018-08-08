package com.networknt.client.builder;

import io.undertow.client.ClientRequest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class HttpClientRequest {
    private ServiceDef serviceDef;
    private Boolean isHttp2Enabled = true;
    private ClientRequest clientRequest;
    private Boolean addCCToken = false;
    private Boolean propagateHeaders = false;
    private CountDownLatch latch;
    private TimeoutDef connectionRequestTimeout = new TimeoutDef(2, TimeUnit.SECONDS);
    private TimeoutDef requestTimeout = new TimeoutDef(1, TimeUnit.SECONDS);
    private long connectionCacheTTLms = 10000;

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
}
