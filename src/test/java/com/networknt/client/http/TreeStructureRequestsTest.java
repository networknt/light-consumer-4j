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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;

public class TreeStructureRequestsTest {
    @ClassRule
    public static TestServer server = TestServer.getInstance();

    static final Logger logger = LoggerFactory.getLogger(TreeStructureRequestsTest.class);
    static final boolean enableHttp2 = server.getServerConfig().isEnableHttp2();
    static final boolean enableHttps = server.getServerConfig().isEnableHttps();
    static final int httpPort = server.getServerConfig().getHttpPort();
    static final int httpsPort = server.getServerConfig().getHttpsPort();
    static final String url = enableHttp2 || enableHttps ? "https://localhost:" + httpsPort : "http://localhost:" + httpPort;
    static final URI uri = URI.create(url);

    Http2ServiceRequest petByIdRequest;
    Http2ServiceRequest requestForWrongType;
    Http2ServiceRequest anotherPetByIdRequest;


    @Before
    public void setUp() throws Exception{
        petByIdRequest = new Http2ServiceRequest(
                this.uri, "/v1/pets/111",
                HttpVerb.GET);

        requestForWrongType = new Http2ServiceRequest(
                this.uri, "/v1/pets/111",
                HttpVerb.GET);


        anotherPetByIdRequest = new Http2ServiceRequest(
                this.uri, "/v1/pets/222",
                HttpVerb.GET);

    }

    @Test
    public void testAsync() {

        CompletableFuture<Pet> petCompletableFuture = petByIdRequest.callForTypedObject(Pet.class);

        CompletableFuture<Pet> anotherPetCompletableFuture = anotherPetByIdRequest.callForTypedObject(Pet.class);

        List<Pet> petList = new LinkedList<>();
        CompletableFuture<Void> petListFutureResponse =
                CompletableFuture.allOf(petCompletableFuture, anotherPetCompletableFuture).exceptionally(e -> {
                    fail();
                    e.printStackTrace();
                    return null;
                }).thenAcceptAsync(aVoid -> {
                    try {
                        petList.add(petCompletableFuture.get());
                    } catch (Exception e) {
                        fail();
                        e.printStackTrace();
                    }

                    try {
                        petList.add(anotherPetCompletableFuture.get());
                    } catch (Exception e) {
                        fail();
                        e.printStackTrace();
                    }

                });

        CompletableFuture<Error> unCompletableFutureError = requestForWrongType.callForTypedObject(Error.class);

        CompletableFuture.allOf(petListFutureResponse, unCompletableFutureError).exceptionally(e -> {
            e.printStackTrace();
            return null;
        }).thenAcceptAsync(aVoid -> {
            assertNotNull(petList);
            assertEquals(2, petList.size());

            Error noObject = null;
            try {
                noObject = unCompletableFutureError.get();
                fail();
            } catch (Exception e) {
                e.printStackTrace();
            }


            assertNull(noObject);
        }).join();

    }



}
