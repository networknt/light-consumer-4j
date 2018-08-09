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

public class TestHttpClientBuilder {

    @Test @Ignore
    public void testHttpClientBuilder() throws ApiException, ClientException, InterruptedException, URISyntaxException, TimeoutException, ExecutionException {
        ClientResponse clientResponse = new HttpClientBuilder()
                .setServiceDef(new ServiceDef("https", "com.cibc.hello-3.0.1", null, null))
                .setClientRequest(new ClientRequest().setPath("/v1/hello").setMethod(Methods.GET))
                .setLatch(new CountDownLatch(1))
                .setConnectionCacheTTLms(10000)
                .build();

        System.out.println(clientResponse.getAttachment(Http2Client.RESPONSE_BODY));

        // Verify connection created.

        clientResponse = new HttpClientBuilder()
                .setServiceDef(new ServiceDef("https", "com.cibc.hello-3.0.1", null, null))
                .setClientRequest(new ClientRequest().setPath("/v1/hello").setMethod(Methods.GET))
                .setLatch(new CountDownLatch(1))
                .build();

        // Verify connection reused.
        System.out.println(clientResponse.getAttachment(Http2Client.RESPONSE_BODY));

        Thread.sleep(10000); // wait for connection to die

        clientResponse = new HttpClientBuilder()
                .setServiceDef(new ServiceDef("https", "com.cibc.hello-3.0.1", null, null))
                .setClientRequest(new ClientRequest().setPath("/v1/hello").setMethod(Methods.GET))
                .setLatch(new CountDownLatch(1))
                .build();

        // Verify connection recreated.
        System.out.println(clientResponse.getAttachment(Http2Client.RESPONSE_BODY));
    }
}
