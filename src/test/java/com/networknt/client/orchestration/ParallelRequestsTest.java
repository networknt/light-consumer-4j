package com.networknt.client.orchestration;


import com.networknt.client.model.HttpVerb;
import com.networknt.petstore.handler.TestServer;
import com.networknt.petstore.model.Pet;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class ParallelRequestsTest {
    @ClassRule
    public static TestServer server = TestServer.getInstance();

    static final Logger logger = LoggerFactory.getLogger(ParallelRequestsTest.class);
    static final boolean enableHttp2 = server.getServerConfig().isEnableHttp2();
    static final boolean enableHttps = server.getServerConfig().isEnableHttps();
    static final int httpPort = server.getServerConfig().getHttpPort();
    static final int httpsPort = server.getServerConfig().getHttpsPort();
    static final String url = enableHttp2 || enableHttps ? "https://localhost:" + httpsPort : "http://localhost:" + httpPort;

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
                new URI(url), "/v1/pets/111",
                HttpVerb.GET);

        requestForWrongType = new Http2ServiceRequest(
                new URI(url), "/v1/pets/111",
                HttpVerb.GET);

        connectionRefusedRequest = new Http2ServiceRequest(
                URI.create("https://localhost:1111/dgp/clientdetail"),
                HttpVerb.GET);


        wrongDomainRequest = new Http2ServiceRequest(
                URI.create("https://garbage/v1/pets/11"),
                HttpVerb.GET);

        petListRequest = new Http2ServiceRequest(
                new URI(url), "/v1/pets",
                HttpVerb.GET);

    }

    @Test
    public void testAsync() {
        CompletableFuture<Pet> onePetFutureResponse = petByIdRequest.callForTypedObject(Pet.class);

        CompletableFuture<Error> unCompletableFutureError = requestForWrongType.callForTypedObject(Error.class);

        CompletableFuture<Pet> futureConnectionRefusedResponse = connectionRefusedRequest.callForTypedObject(Pet.class);

        CompletableFuture<Http2ServiceResponse> futureNoResponseExcepted = wrongDomainRequest.call();

        CompletableFuture<List<Pet>> petListFutureResponse = petListRequest.callForTypedList(Pet.class);

        try {
            CompletableFuture.allOf(onePetFutureResponse, unCompletableFutureError, futureNoResponseExcepted, futureConnectionRefusedResponse, petListFutureResponse).get();
            Assert.fail();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            unCompletableFutureError.get();
            Assert.fail();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            futureNoResponseExcepted.get();
            Assert.fail();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            futureConnectionRefusedResponse.get();
            Assert.fail();
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.aPet = onePetFutureResponse.join();
        Assert.assertNotNull(this.aPet);
        logger.info("Pet Id" + this.aPet.getId());
        this.aPetList = petListFutureResponse.join();
        Assert.assertNotNull(this.aPetList);
        logger.info("pet list size" + this.aPetList.size());
    }



    @Test
    public void testCallback() {
        Http2ServiceRequest.CallWaiter clientCallWaiter = petByIdRequest.callForTypedObject(Pet.class,
                receivedClient -> aPet = receivedClient, e -> e.printStackTrace());
        Http2ServiceRequest.CallWaiter peopleCallWaiter = petByIdRequest.callForTypedObject(Error.class,
                notConvertedNullClient -> err = notConvertedNullClient, e -> e.printStackTrace());
        Http2ServiceRequest.CallWaiter connectionRefusedCallWaiter = connectionRefusedRequest.callForTypedObject(Pet.class,
                receivedClient -> nullPet = receivedClient, e -> e.printStackTrace());
        Http2ServiceRequest.CallWaiter noHopeCallWaiter = wrongDomainRequest.call(
                receivedResponse -> nullResponse = receivedResponse, e -> e.printStackTrace());
        Http2ServiceRequest.CallWaiter clientListCallWaiter = petListRequest.callForTypedList(Pet.class,
                receivedClientList-> aPetList = receivedClientList, e -> e.printStackTrace());
        Stream.of(clientCallWaiter, peopleCallWaiter, connectionRefusedCallWaiter, noHopeCallWaiter, clientListCallWaiter).forEach(Http2ServiceRequest.CallWaiter::waitForResponse);
        Assert.assertNotNull(this.aPet);
        logger.info("Pet Id" + this.aPet.getId());
        Assert.assertNull(err);
        Assert.assertNull(nullPet);
        Assert.assertNull(nullResponse);
        Assert.assertNotNull(this.aPetList);
        System.out.println(aPetList.size());
    }


}
