package com.networknt.client.builder;

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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TestHttpClientBuilder {

    @Test @Ignore
    public void testHttpClientBuilder() throws ApiException, ClientException, InterruptedException, URISyntaxException, TimeoutException, ExecutionException {
        ClientResponse clientResponse = new HttpClientBuilder()
                .setServiceDef(new ServiceDef("https", "com.cibc.hello-3.0.1", null, null))
                .setClientRequest(new ClientRequest().setPath("/v1/hello").setMethod(Methods.GET))
                .setLatch(new CountDownLatch(1))
                .setRequestTimeout(new TimeoutDef(1000,TimeUnit.SECONDS))
                .build();

        System.out.println(clientResponse);
    }
}
