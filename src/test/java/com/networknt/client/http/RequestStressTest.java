package com.networknt.client.http;


import com.networknt.client.model.HttpVerb;
import com.networknt.petstore.handler.TestServer;
import com.networknt.petstore.model.Pet;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RequestStressTest {
    @ClassRule
    public static TestServer server = TestServer.getInstance();

    static final Logger logger = LoggerFactory.getLogger(RequestStressTest.class);
    static final boolean enableHttp2 = server.getServerConfig().isEnableHttp2();
    static final boolean enableHttps = server.getServerConfig().isEnableHttps();
    static final int httpPort = server.getServerConfig().getHttpPort();
    static final int httpsPort = server.getServerConfig().getHttpsPort();
    static final String url = enableHttp2 || enableHttps ? "https://localhost:" + httpsPort : "http://localhost:" + httpPort;
    static final URI uri = URI.create(url);

    List<Http2ServiceRequest> requestList = new LinkedList<>();

    @Before
    public void setUp() throws Exception{
        IntStream.rangeClosed(1, 99).forEach(id->requestList.add(new Http2ServiceRequest(
                this.uri, "/v1/pets/"+id,
                HttpVerb.GET)));
    }

    @Test
    public void loadTestRequests() {
        Instant start = Instant.now();
        List<CompletableFuture<Pet>> completableFutureList = requestList.parallelStream()
                .map(http2ServiceRequest -> http2ServiceRequest.callForTypedObject(Pet.class))
                .collect(Collectors.toList());
        Instant called = Instant.now();
        System.out.println("start-called in millis " + Duration.between(start, called).toMillis());
        CompletableFuture.allOf(
                completableFutureList.toArray(new CompletableFuture[completableFutureList.size()]))
                .join();
        Instant completed = Instant.now();
        System.out.println("called-completed in millis " + Duration.between(called, completed).toMillis());
//        completableFutureList.stream().map(CompletableFuture::join).forEach(System.out::println);
    }
}
