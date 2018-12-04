package com.networknt.client.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.client.Http2Client;

import com.networknt.client.builder.HttpClientBuilder;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Methods;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestLightRestClient {



    private static LightRestClient lightRestClient;
    static final Logger logger = LoggerFactory.getLogger(TestLightRestClient.class);
    @BeforeClass
    public static void setUp() {
        lightRestClient = new LightRestClient();
    }

    @Test @Ignore
    public void testGetMethod() throws RestClientException, Exception {

        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI("https://localhost:8443"), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL,  OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/v1/pets").setMethod(Methods.GET);

            connection.sendRequest(request, client.createClientCallback(reference, latch));

            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        int statusCode = reference.get().getResponseCode();
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        Assert.assertEquals(200, statusCode);
        Assert.assertNotNull(body);

    }


    @Test @Ignore
    public void testGet() throws RestClientException, Exception {
        String str = lightRestClient.get("https://localhost:8443", "/v1/pets/1", String.class);

        System.out.println(str);
        assertNotNull(str);
    }

    @Test @Ignore
    public void testGetWithType() throws RestClientException, Exception {
        Pet pet = lightRestClient.get("https://localhost:8443", "/v1/pets/1", Pet.class);
        assertTrue(pet.getId()==1);
    }


    @Test @Ignore
    public void testPost() throws RestClientException, JsonProcessingException {
        Pet pet = new Pet();
        pet.setId(1L);
        pet.setName("cat");
        pet.setTag("tag1");
        String requestBody = Config.getInstance().getMapper().writeValueAsString(pet);
        String str = lightRestClient.post("https://localhost:8443", "/v1/pets", requestBody);
        assertNotNull(str);
    }

    @Test@Ignore
    public void testAuthToken(){
        HttpClientBuilder httpClientBuilder= new HttpClientBuilder();
        httpClientBuilder.setAuthToken("1234abc");
        Assert.assertEquals("1234abc",httpClientBuilder.getAuthToken());
    }
}
