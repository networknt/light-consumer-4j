package com.networknt.client.orchestration;
import com.networknt.client.model.HttpVerb;
import com.networknt.petstore.handler.TestServer;
import com.networknt.petstore.model.Pet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ParallelSerialHybridRequestsTest {
    @ClassRule
    public static TestServer server = TestServer.getInstance();

    static final Logger logger = LoggerFactory.getLogger(ParallelSerialHybridRequestsTest.class);
    static final boolean enableHttp2 = server.getServerConfig().isEnableHttp2();
    static final boolean enableHttps = server.getServerConfig().isEnableHttps();
    static final int httpPort = server.getServerConfig().getHttpPort();
    static final int httpsPort = server.getServerConfig().getHttpsPort();
    static final String url = enableHttp2 || enableHttps ? "https://localhost:" + httpsPort : "http://localhost:" + httpPort;
    static final URI uri = URI.create(url);

    Http2ServiceRequest petListRequest;

    List<Pet> aPetList;
    List<Pet> reversedPetList;
    Pet firstPet;
    Pet secondPet;

    @Before
    public void setUp() throws Exception{

        petListRequest = new Http2ServiceRequest(
                this.uri, "/v1/pets",
                HttpVerb.GET);

    }

    @Test
    public void testAsync() {
        CompletableFuture<List<Pet>> petListFutureResponse = petListRequest.callForTypedList(Pet.class);

        CompletableFuture<Pet> firstPetCompletableFuture = petListFutureResponse.thenComposeAsync(pets -> new Http2ServiceRequest(
                this.uri, "/v1/pets/" + pets.get(0).getId(),
                HttpVerb.GET).callForTypedObject(Pet.class));

        CompletableFuture<Pet> secondPetCompletableFuture = petListFutureResponse.thenComposeAsync(pets -> new Http2ServiceRequest(
                this.uri, "/v1/pets/" + pets.get(1).getId(),
                HttpVerb.GET).callForTypedObject(Pet.class));

        CompletableFuture<List<Pet>> reversedFuturePetList = secondPetCompletableFuture.thenCombineAsync(firstPetCompletableFuture,
                (firstPet, secondPet) -> Arrays.asList(secondPet, firstPet));


        try {
            reversedFuturePetList.get();
        } catch (Exception e) {
            Assert.fail();
        }
        this.aPetList = petListFutureResponse.join();
        this.reversedPetList = reversedFuturePetList.join();

        this.firstPet = firstPetCompletableFuture.join();
        this.secondPet = secondPetCompletableFuture.join();
        Assert.assertNotNull(aPetList);
        Assert.assertNotNull(reversedPetList);
        Assert.assertNotNull(firstPet);
        Assert.assertNotNull(secondPet);
        Assert.assertEquals(aPetList.size(), reversedPetList.size());
        System.out.println(aPetList.size());

    }


}
