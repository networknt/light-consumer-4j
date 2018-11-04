package com.networknt.client.rest;

import com.networknt.client.Http2Client;
import com.networknt.client.builder.HttpClientBuilder;
import com.networknt.config.Config;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

public class LightRestClient implements RestClient{

    private static Logger logger = LoggerFactory.getLogger(LightRestClient.class);

    @Override
    public  String get(String url, String path) throws RestClientException {
        return get(url, path, String.class);
    }

    @Override
    public  <T> T get(String url, String path, Class<T> responseType) throws RestClientException {
        return get(url, path, responseType, null);
    }

    @Override
    public <T> T get(String url,  String path, Class<T> responseType, Map<String, ?> headerMap) throws RestClientException {
        return execute(url, path, responseType, headerMap, Methods.GET, null);
    }

    @Override
    public <T> T post(String url, String path, Class<T> responseType, String requestBody) throws RestClientException {
        return post(url, path, responseType, null, requestBody);
    }

    @Override
    public  String post(String url, String path,  String requestBody) throws RestClientException {
        return post(url, path, String.class, requestBody);
    }

    @Override
    public <T> T post(String url,  String path, Class<T> responseType, Map<String, ?> headerMap,  String requestBody) throws RestClientException {
        return execute(url, path, responseType, headerMap, Methods.POST, requestBody);
    }

    @Override
    public  String put(String url, String path,  String requestBody) throws RestClientException {
        return put(url, path, null, requestBody);
    }

    @Override
    public  String put(String url,  String path,  Map<String, ?> headerMap,  String requestBody) throws RestClientException {
         return execute(url, path, String.class, headerMap, Methods.PUT, requestBody);
    }

    @Override
    public  String delete(String url, String path) throws RestClientException {
        return delete(url, path, null, null);
    }

    @Override
    public String delete(String url,  String path,  Map<String, ?> headerMap,  String requestBody) throws RestClientException {
        return execute(url, path, String.class, headerMap, Methods.DELETE, requestBody);
    }

    protected <T> T execute (String url,  String path, Class<T> responseType, Map<String, ?> headerMap, HttpString method, String requestBody) throws RestClientException {
        try {
            Future<ClientResponse> clientRequest = new HttpClientBuilder()
                    .setClientRequest(new ClientRequest().setPath(path).setMethod(method))
                    .setApiHost(url)
                    .setHeaderMap(headerMap)
                    .setRequestBody(requestBody)
                    .setLatch(new CountDownLatch(1))
                    .setConnectionCacheTTLms(10000)
                    .send();

            ClientResponse clientResponse = clientRequest.get();
            return Config.getInstance().getMapper().readValue(clientResponse.getAttachment(Http2Client.RESPONSE_BODY), responseType);

        } catch (Exception e) {
            logger.error("Error occurred when calling service.", e);
            throw new RestClientException("Light restful service call exception;" + e);
        }
    }
}
