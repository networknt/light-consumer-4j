package com.networknt.client.builder;

import com.networknt.client.Http2Client;
import com.networknt.client.model.ServiceDef;
import com.networknt.exception.ApiException;
import com.networknt.exception.ClientException;
import com.networknt.registry.Registry;
import com.networknt.registry.URLImpl;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Methods;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

public class TestHttpClientBuilder {

    private static Registry registry = (Registry) SingletonServiceFactory.getBean(Registry.class);

    @Test @Ignore
    public void testHttpClientBuilder() throws ApiException, ClientException, InterruptedException, URISyntaxException, TimeoutException, ExecutionException {
        Future<ClientResponse> clientRequest = new HttpClientBuilder()
                .setServiceDef(new ServiceDef("https", "com.networknt.hello-1", null))
                .setClientRequest(new ClientRequest().setPath("/v1/customers/1").setMethod(Methods.GET))
                .setLatch(new CountDownLatch(1))
                .setConnectionCacheTTLms(10000)
                .send();

        ClientResponse clientResponse = clientRequest.get();
        System.out.println(clientResponse.getAttachment(Http2Client.RESPONSE_BODY));

        // Verify connection created.

        clientRequest = new HttpClientBuilder()
                .setServiceDef(new ServiceDef("https", "com.networknt.hello-1", null))
                .setClientRequest(new ClientRequest().setPath("/v1/customers/1").setMethod(Methods.GET))
                .setLatch(new CountDownLatch(1))
                .send();

        // Verify connection reused.
        clientResponse = clientRequest.get();
        System.out.println(clientResponse.getAttachment(Http2Client.RESPONSE_BODY));

        Thread.sleep(10000); // wait for connection to die

        clientRequest = new HttpClientBuilder()
                .setServiceDef(new ServiceDef("https", "com.networknt.hello-1", null))
                .setClientRequest(new ClientRequest().setPath("/v1/customers/1").setMethod(Methods.GET))
                .setLatch(new CountDownLatch(1))
                .send();

        // Verify connection recreated.
        clientResponse = clientRequest.get();
        System.out.println(clientResponse.getAttachment(Http2Client.RESPONSE_BODY));
    }

    @Test @Ignore
    public void testHttpClientBuilderServiceUrl()  {

        // This test cannot be executed. Need to mock registry and load balancing

        registry.register(URLImpl.valueOf("https://localhost:8080/direct?group=default"));
        HttpClientBuilder clientRequest = new HttpClientBuilder()
                .setServiceDef(new ServiceDef("https", "com.networknt.hello-1", null))
                .setClientRequest(new ClientRequest().setPath("/v1/hello").setMethod(Methods.GET))
                .setLatch(new CountDownLatch(1))
                .setConnectionCacheTTLms(10000);

        Assert.assertEquals("https://localhost:8080", clientRequest.getServiceUrl());
    }

}
