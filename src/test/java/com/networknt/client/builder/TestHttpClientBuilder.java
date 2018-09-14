package com.networknt.client.builder;

import com.networknt.client.Http2Client;
import com.networknt.exception.ApiException;
import com.networknt.exception.ClientException;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Methods;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class TestHttpClientBuilder {

    @Test @Ignore
    public void testHttpClientBuilder() throws ApiException, ClientException, InterruptedException, URISyntaxException, TimeoutException, ExecutionException {
        HttpClientRequest clientRequest = new HttpClientBuilder()
                .setServiceDef(new ServiceDef("https", "com.networknt.hello-1", null))
                .setClientRequest(new ClientRequest().setPath("/v1/hello").setMethod(Methods.GET))
                .setLatch(new CountDownLatch(1))
                .setConnectionCacheTTLms(10000)
                .send();

        ClientResponse clientResponse = clientRequest.awaitResponse();
        System.out.println(clientResponse.getAttachment(Http2Client.RESPONSE_BODY));

        // Verify connection created.

        clientRequest = new HttpClientBuilder()
                .setServiceDef(new ServiceDef("https", "com.networknt.hello-1", null))
                .setClientRequest(new ClientRequest().setPath("/v1/hello").setMethod(Methods.GET))
                .setLatch(new CountDownLatch(1))
                .send();

        // Verify connection reused.
        clientResponse = clientRequest.awaitResponse();
        System.out.println(clientResponse.getAttachment(Http2Client.RESPONSE_BODY));

        Thread.sleep(10000); // wait for connection to die

        clientRequest = new HttpClientBuilder()
                .setServiceDef(new ServiceDef("https", "com.networknt.hello-1", null))
                .setClientRequest(new ClientRequest().setPath("/v1/hello").setMethod(Methods.GET))
                .setLatch(new CountDownLatch(1))
                .send();

        // Verify connection recreated.
        clientResponse = clientRequest.awaitResponse();
        System.out.println(clientResponse.getAttachment(Http2Client.RESPONSE_BODY));
    }
}
