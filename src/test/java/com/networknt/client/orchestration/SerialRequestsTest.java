package com.networknt.client.orchestration;


import com.networknt.client.model.HttpVerb;
import com.networknt.petstore.handler.TestServer;
import com.networknt.petstore.model.Pet;
import com.networknt.petstore.model.Tag;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SerialRequestsTest {
    @ClassRule
    public static TestServer server = TestServer.getInstance();

    static final Logger logger = LoggerFactory.getLogger(SerialRequestsTest.class);
    static final boolean enableHttp2 = server.getServerConfig().isEnableHttp2();
    static final boolean enableHttps = server.getServerConfig().isEnableHttps();
    static final int httpPort = server.getServerConfig().getHttpPort();
    static final int httpsPort = server.getServerConfig().getHttpsPort();
    static final String url = enableHttp2 || enableHttps ? "https://localhost:" + httpsPort : "http://localhost:" + httpPort;
    static final URI uri = URI.create(url);

    Http2ServiceRequest petByIdRequest;
    Http2ServiceRequest requestForWrongType;
    Http2ServiceRequest petListRequest;
    Http2ServiceRequest connectionRefusedRequest;
    Http2ServiceRequest wrongDomainRequest;

    Pet aPet;
    Error err;

    Pet nullPet = null;
    Http2ServiceResponse nullResponse = null;
    List<Pet> aPetList;

    @Before
    public void setUp() throws Exception{
        petByIdRequest = new Http2ServiceRequest(
                this.uri, "/v1/pets/111",
                HttpVerb.GET);

        requestForWrongType = new Http2ServiceRequest(
                this.uri, "/v1/pets/111",
                HttpVerb.GET);

        connectionRefusedRequest = new Http2ServiceRequest(
                URI.create("https://localhost:1111/v1/pets/11"),
               HttpVerb.GET);

        wrongDomainRequest = new Http2ServiceRequest(
                URI.create("https://garbage/v1/pets/11"),
                HttpVerb.GET);

        petListRequest = new Http2ServiceRequest(
                this.uri, "/v1/pets",
                HttpVerb.GET);

    }

    @Test
    public void testAsync() {
        CompletableFuture<List<Pet>> petListFutureResponse = petListRequest.callForTypedList(Pet.class);

        CompletableFuture<Pet> petCompletableFuture = petListFutureResponse.thenComposeAsync(pets -> new Http2ServiceRequest(
                this.uri, "/v1/pets/" + pets.get(0).getId(),
                HttpVerb.GET).callForTypedObject(Pet.class));

        CompletableFuture<Tag> tagCompletableFuture = petCompletableFuture.thenComposeAsync(pet -> new Http2ServiceRequest(
                this.uri, "/v1/tags/1" + pet.getTag(),
                HttpVerb.GET).callForTypedObject(Tag.class));

        try {
            tagCompletableFuture.get();
            Assert.fail();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.aPetList = petListFutureResponse.join();
        this.aPet = petCompletableFuture.join();
        Assert.assertNotNull(aPetList);
        System.out.println(aPetList.size());
        Assert.assertNotNull(aPet);
        System.out.println(aPet.getId());

    }



}
