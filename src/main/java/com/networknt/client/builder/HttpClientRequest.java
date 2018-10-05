package com.networknt.client.builder;

import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class HttpClientRequest implements AutoCloseable {
    private ServiceDef serviceDef;
    private Boolean isHttp2Enabled = true;
    private ClientRequest clientRequest;
    private String requestBody;
    private Boolean addCCToken = false;
    private Boolean propagateHeaders = false;
    private CountDownLatch latch;
    private TimeoutDef connectionRequestTimeout = new TimeoutDef(5, TimeUnit.SECONDS);
    private TimeoutDef requestTimeout = new TimeoutDef(5, TimeUnit.SECONDS);
    private long connectionCacheTTLms = 10000;
    private AtomicReference<ClientResponse> responseReference;

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
}
