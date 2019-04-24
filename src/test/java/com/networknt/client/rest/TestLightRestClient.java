/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.client.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.client.Http2Client;

import com.networknt.client.builder.HttpClientBuilder;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import com.networknt.petstore.handler.TestServer;
import com.networknt.petstore.model.Pet;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Methods;
import org.junit.*;
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

    @ClassRule
    public static TestServer server = TestServer.getInstance();
    static final boolean enableHttp2 = server.getServerConfig().isEnableHttp2();
    static final boolean enableHttps = server.getServerConfig().isEnableHttps();
    static final int httpPort = server.getServerConfig().getHttpPort();
    static final int httpsPort = server.getServerConfig().getHttpsPort();
    static final String url = enableHttp2 || enableHttps ? "https://localhost:" + httpsPort : "http://localhost:" + httpPort;

    private static LightRestClient lightRestClient;
    static final Logger logger = LoggerFactory.getLogger(TestLightRestClient.class);
    @BeforeClass
    public static void setUp() {
        lightRestClient = new LightRestClient();
    }

    @Test
    public void testGetMethod() throws RestClientException, Exception {

        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL,  OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
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


    @Test
    public void testGet() throws RestClientException, Exception {
        String str = lightRestClient.get(url, "/v1/pets/1", String.class);

        System.out.println(str);
        assertNotNull(str);
    }

    @Test
    public void testGetWithType() throws RestClientException, Exception {
        Pet pet = lightRestClient.get(url, "/v1/pets/1", Pet.class);
        assertTrue(pet.getId()==1);
    }


    @Test
    public void testPost() throws RestClientException, JsonProcessingException {
        Pet pet = new Pet();
        pet.setId(1L);
        pet.setName("cat");
        pet.setTag("tag1");
        String requestBody = Config.getInstance().getMapper().writeValueAsString(pet);
        String str = lightRestClient.post(url, "/v1/pets", requestBody);
        assertNotNull(str);
    }

    @Test
    public void testAuthToken(){
        HttpClientBuilder httpClientBuilder= new HttpClientBuilder();
        httpClientBuilder.setAuthToken("1234abc");
        Assert.assertEquals("1234abc",httpClientBuilder.getAuthToken());
    }
}
