package com.networknt.client.builder;

import com.networknt.client.Http2Client;
import com.networknt.client.model.ConsumerConfig;
import com.networknt.cluster.Cluster;
import com.networknt.config.Config;
import com.networknt.exception.ApiException;
import com.networknt.exception.ClientException;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.client.ClientCallback;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientExchange;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class HttpClientBuilder {

    private static Logger logger = LoggerFactory.getLogger(HttpClientBuilder.class);
    private static Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
    private static Http2Client client = Http2Client.getInstance();
    private HttpClientRequest httpClientRequest;
    private static ConnectionCacheManager connectionCacheManager = new ConnectionCacheManager();
    private static final String CONFIG_NAME = "consumer";
    private static final ConsumerConfig config = (ConsumerConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ConsumerConfig.class);

    /**
     * Builder for issuing the request to the client.
     *
     * @return The response from the request.
     * @throws URISyntaxException If a bad uri is provided for the given resource.
     * @throws InterruptedException Could occur when establishing a connection.
     * @throws ClientException Could occur when requesting a jwt an a connection couldn't be established to oauth
     * @throws ApiException When requesting a JWT
     * @throws TimeoutException If a timeout occurs in establishing a connection to the service.
     * @throws ExecutionException If an issue other then a timeout occurs in establishing a connection to the service.
     */
    public Future<ClientResponse> send() throws URISyntaxException, InterruptedException, ApiException, TimeoutException, ExecutionException, ClientException {
        // Get a reference to the response
        httpClientRequest.setResponseReference(new AtomicReference<>());

        // Include the access token
        if (httpClientRequest.getAddCCToken()) {
            ClientRequest clientRequest = httpClientRequest.getClientRequest();
            client.addCcToken(clientRequest);
            httpClientRequest.setClientRequest(clientRequest);
        }

        ClientConnection clientConnection = connectionCacheManager.getConnection(this.getRequestHost(),
                httpClientRequest.getConnectionCacheTTLms(), httpClientRequest.getConnectionRequestTimeout(),
                httpClientRequest.getHttp2Enabled());

        // Send the request
        clientConnection.sendRequest(httpClientRequest.getClientRequest(), this.getClientCallback(httpClientRequest.getResponseReference()));

        // Start a thread to wait for the timeout if provided.
        return this.httpClientRequest.triggerLatchAwait();
    }

    /**
     * Gets the URI to the server it will connect to using the cluster load balancer.
     *
     * @return
     * @throws URISyntaxException
     */
    private URI getRequestHost() throws URISyntaxException {
        return new URI(cluster.serviceToUrl(httpClientRequest.getServiceDef().getProtocol(),
                httpClientRequest.getServiceDef().getServiceId(), httpClientRequest.getServiceDef().getEnvironment(),
                httpClientRequest.getServiceDef().getRequestKey()));
    }

    private ClientCallback<ClientExchange> getClientCallback(AtomicReference<ClientResponse> reference) {
        if (httpClientRequest.getRequestBody() == null) {
            return client.createClientCallback(reference, httpClientRequest.getLatch());
        }
        return client.createClientCallback(reference, httpClientRequest.getLatch(), httpClientRequest.getRequestBody());
    }

    public HttpClientBuilder() {
        this.httpClientRequest = new HttpClientRequest();
    }

    public HttpClientBuilder setServiceDef(ServiceDef serviceDef) {
        if (serviceDef.getEnvironment() == null) { // get env from service.yml config
            String env = config.getServiceEnv().get(serviceDef.getServiceId());
            if (env != null && env.length() > 0) {
                serviceDef.setEnvironment(env);
            } else {
                throw new RuntimeException("Service \"" + serviceDef.getServiceId() + "\" was not configured with an environment.");
            }
        }
        this.httpClientRequest.setServiceDef(serviceDef);
        return this;
    }

    public HttpClientBuilder disableHttp2() {
        this.httpClientRequest.setHttp2Enabled(false);
        return this;
    }

    public HttpClientBuilder setClientRequest(ClientRequest clientRequest) {
        this.httpClientRequest.setClientRequest(clientRequest);
        return this;
    }

    public HttpClientBuilder setRequestBody(String requestBody) {
        this.httpClientRequest.setRequestBody(requestBody);
        return this;
    }

    public HttpClientBuilder addCCToken() {
        this.httpClientRequest.setAddCCToken(true);
        return this;
    }

    public HttpClientBuilder propagateHeaders() {
        this.httpClientRequest.setPropagateHeaders(true);
        return this;
    }

    public HttpClientBuilder setLatch(CountDownLatch latch) {
        this.httpClientRequest.setLatch(latch);
        return this;
    }

    public HttpClientBuilder setConnectionRequestTimeout(TimeoutDef timeout) {
        this.httpClientRequest.setConnectionRequestTimeout(timeout);
        return this;
    }

    public HttpClientBuilder setRequestTimeout(TimeoutDef timeout) {
        this.httpClientRequest.setRequestTimeout(timeout);
        return this;
    }

    public HttpClientBuilder setConnectionCacheTTLms(long connectionCacheTTLms) {
        this.httpClientRequest.setConnectionCacheTTLms(connectionCacheTTLms);
        return this;
    }
}
