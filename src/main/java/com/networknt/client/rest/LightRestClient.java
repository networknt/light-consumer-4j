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

import com.networknt.client.Http2Client;
import com.networknt.client.builder.HttpClientBuilder;
import com.networknt.client.builder.ServiceDef;
import com.networknt.config.Config;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

public class LightRestClient implements RestClient {

    private static Logger logger = LoggerFactory.getLogger(LightRestClient.class);

    private OptionMap restOptions;

    /**
     * Instantiate a new LightRestClient with default RestClientOptions
     */
    public LightRestClient() {
        this.restOptions = OptionMap.EMPTY;
    }

    /**
     * Instantiate a new LightRestClient with configurable RestClientOptions
     * @param restOptions org.xnio.OptionMap of RestClientOptions
     */
    public LightRestClient(OptionMap restOptions) {
        this.restOptions = restOptions != null ? restOptions : OptionMap.EMPTY;
    }

    @Override
    public String get(String url, String path) throws RestClientException {
        return get(url, path, String.class);
    }

    @Override
    public <T> T get(String url, String path, Class<T> responseType) throws RestClientException {
        return get(url, path, responseType, null);
    }

    @Override
    public <T> T get(ServiceDef serviceDef, String path, Class<T> responseType) throws RestClientException {
        return execute(serviceDef, path, responseType, null, Methods.GET, null);
    }

    @Override
    public String get(ServiceDef serviceDef, String path) throws RestClientException {
        return get(serviceDef, path, String.class);
    }

    @Override
    public <T> T get(String url, String path, Class<T> responseType, Map<String, ?> headerMap) throws RestClientException {
        return execute(url, path, responseType, headerMap, Methods.GET, null);
    }

    @Override
    public <T> T post(String url, String path, Class<T> responseType, String requestBody) throws RestClientException {
        return post(url, path, responseType, null, requestBody);
    }

    @Override
    public String post(String url, String path, String requestBody) throws RestClientException {
        return post(url, path, String.class, requestBody);
    }

    @Override
    public <T> T post(String url, String path, Class<T> responseType, Map<String, ?> headerMap, String requestBody) throws RestClientException {
        return execute(url, path, responseType, headerMap, Methods.POST, requestBody);
    }

    @Override
    public String post(ServiceDef serviceDef, String path, String requestBody) throws RestClientException {
        return post(serviceDef, path, String.class, requestBody);
    }

    @Override
    public <T> T post(ServiceDef serviceDef, String path, Class<T> responseType, String requestBody) throws RestClientException {
        return post(serviceDef, path, responseType, null, requestBody);
    }

    @Override
    public <T> T post(ServiceDef serviceDef, String path, Class<T> responseType, Map<String, ?> headerMap, String requestBody) throws RestClientException {
        return execute(serviceDef, path, responseType, headerMap, Methods.POST, requestBody);
    }

    @Override
    public String put(String url, String path, String requestBody) throws RestClientException {
        return put(url, path, null, requestBody);
    }

    @Override
    public String put(String url, String path, Map<String, ?> headerMap, String requestBody) throws RestClientException {
        return execute(url, path, String.class, headerMap, Methods.PUT, requestBody);
    }

    @Override
    public String put(ServiceDef serviceDef, String path, String requestBody) throws RestClientException {
        return execute(serviceDef, path, String.class, null, Methods.PUT, requestBody);
    }

    @Override
    public String put(ServiceDef serviceDef, String path, Map<String, ?> headerMap, String requestBody) throws RestClientException {
        return execute(serviceDef, path, String.class, headerMap, Methods.PUT, requestBody);
    }

    @Override
    public String delete(String url, String path) throws RestClientException {
        return delete(url, path, null, null);
    }

    @Override
    public String delete(String url, String path, Map<String, ?> headerMap, String requestBody) throws RestClientException {
        return execute(url, path, String.class, headerMap, Methods.DELETE, requestBody);
    }

    @Override
    public String delete(ServiceDef serviceDef, String path) throws RestClientException {
        return execute(serviceDef, path, String.class, null, Methods.DELETE, null);
    }

    protected <T> T execute(String url, String path, Class<T> responseType, Map<String, ?> headerMap, HttpString method, String requestBody) throws RestClientException {
        return this.executeRequest(buildClientRequest()
                .setApiHost(url), path, responseType, headerMap, method, requestBody);
    }

    protected <T> T execute(ServiceDef serviceDef, String path, Class<T> responseType, Map<String, ?> headerMap, HttpString method, String requestBody) throws RestClientException {
        return this.executeRequest(buildClientRequest()
                .setServiceDef(serviceDef), path, responseType, headerMap, method, requestBody);
    }

    private <T> T executeRequest(HttpClientBuilder baseClientRequest, String path, Class<T> responseType, Map<String, ?> headerMap, HttpString method, String requestBody) throws RestClientException {
        try {
            Future<ClientResponse> clientRequest = baseClientRequest
                    .setClientRequest(new ClientRequest().setPath(path).setMethod(method))
                    .setHeaderMap(headerMap)
                    .setRequestBody(requestBody)
                    .send();

            ClientResponse clientResponse = clientRequest.get();

            if (responseType.equals(ClientResponse.class)) {
                return responseType.cast(clientResponse);
            }

            // log response if we don't get full clientResponse for diagnostic purposes
            logger.info(clientResponse.toString());

            if (responseType.equals(String.class)) {
                return responseType.cast(clientResponse.getAttachment(Http2Client.RESPONSE_BODY));
            }
            return Config.getInstance().getMapper().readValue(clientResponse.getAttachment(Http2Client.RESPONSE_BODY), responseType);

        } catch (Exception e) {
            logger.error("Error occurred when calling service.", e);
            throw new RestClientException("Light restful service call exception;" + e);
        }
    }

    private HttpClientBuilder buildClientRequest() {
        HttpClientBuilder clientRequest = new HttpClientBuilder()
                .setLatch(new CountDownLatch(1))
                .setConnectionCacheTTLms(this.restOptions.get(RestClientOptions.CONN_CACHE_TTL, 10000));

        if (this.restOptions.contains(RestClientOptions.ADD_CC_TOKEN) && this.restOptions.get(RestClientOptions.ADD_CC_TOKEN)) {
            clientRequest = clientRequest.addCCToken();
        }

        if (this.restOptions.contains(RestClientOptions.DISABLE_HTTP2) && this.restOptions.get(RestClientOptions.DISABLE_HTTP2)) {
            clientRequest = clientRequest.disableHttp2();
        }

        if (this.restOptions.contains(RestClientOptions.CONN_REQ_TIMEOUT)) {
            clientRequest = clientRequest.setConnectionRequestTimeout(this.restOptions.get(RestClientOptions.CONN_REQ_TIMEOUT));
        }

        if (this.restOptions.contains(RestClientOptions.REQ_TIMEOUT)) {
            clientRequest = clientRequest.setRequestTimeout(this.restOptions.get(RestClientOptions.REQ_TIMEOUT));
        }

        if (this.restOptions.contains(RestClientOptions.MAX_REQ_CNT)) {
            clientRequest = clientRequest.setMaxReqCount(this.restOptions.get(RestClientOptions.MAX_REQ_CNT));
        }

        return clientRequest;
    }
}
