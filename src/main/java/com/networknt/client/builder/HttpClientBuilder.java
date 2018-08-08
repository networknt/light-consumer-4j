package com.networknt.client.builder;

import com.networknt.client.Http2Client;
import com.networknt.cluster.Cluster;
import com.networknt.exception.ApiException;
import com.networknt.exception.ClientException;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientCallback;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientExchange;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class HttpClientBuilder {

    private static Logger logger = LoggerFactory.getLogger(HttpClientBuilder.class);
    private static Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
    private static Http2Client client = Http2Client.getInstance();
    private HttpClientRequest httpClientRequest;

    private static OptionMap http2OptionMap = OptionMap.create(UndertowOptions.ENABLE_HTTP2, true);
    private static OptionMap http1OptionMap = OptionMap.EMPTY;

    private static ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * Builder for issuing the request to the client.
     *
     * @return The response from the request.
     */
    public ClientResponse build() throws URISyntaxException, InterruptedException, ClientException, ApiException, TimeoutException, ExecutionException {
        // Get a reference to the response
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();

        // Include the access token
        if (httpClientRequest.getAddCCToken()) {
            ClientRequest clientRequest = httpClientRequest.getClientRequest();
            client.addCcToken(clientRequest);
            httpClientRequest.setClientRequest(clientRequest);
        }

        // Send the request
        this.getConnection().sendRequest(httpClientRequest.getClientRequest(), this.getClientCallback(reference));

        this.awaitRequest();

        return reference.get();
    }

    /**
     * Get the connection before issuing the request. This method will spawn a thread within the cached thread pool
     * for waiting before timing out.
     *
     * @return The connection.
     * @throws URISyntaxException
     * @throws TimeoutException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private ClientConnection getConnection() throws URISyntaxException, TimeoutException, ExecutionException, InterruptedException {
        Future<ClientConnection> connectionFuture = executorService.submit(new ClientConnectionCallable(this.getRequestHost(), this.getOptionMap()));
        try {
            return connectionFuture.get(this.httpClientRequest.getConnectionTimeout().getTimeout(),
                    this.httpClientRequest.getConnectionTimeout().getUnit());
        } catch (TimeoutException e) {
            logger.error("Timeout occurred when getting connection to: " + this.getRequestHost(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Error occurred when getting connection.", e);
            throw e;
        }
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

    private OptionMap getOptionMap() {
        if (this.httpClientRequest.getHttp2Enabled()) {
            return http2OptionMap;
        }
        return http1OptionMap;
    }

    private ClientCallback<ClientExchange> getClientCallback(AtomicReference<ClientResponse> reference) {
        return client.createClientCallback(reference, httpClientRequest.getLatch());
    }

    /**
     * Helper to use the latch to wait for the request to return within the given timeout (if provided).
     * @throws InterruptedException
     */
    private void awaitRequest() throws InterruptedException {
        if (this.httpClientRequest.getRequestTimeout() != null) {
            this.httpClientRequest.getLatch().await(this.httpClientRequest.getRequestTimeout().getTimeout(), this.httpClientRequest.getRequestTimeout().getUnit());
        } else {
            this.httpClientRequest.getLatch().await();
        }
    }

    public HttpClientBuilder() {
        this.httpClientRequest = new HttpClientRequest();
    }

    public HttpClientBuilder setServiceDef(ServiceDef serviceDef) {
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

    public HttpClientBuilder setConnectionTimeout(TimeoutDef timeout) {
        this.httpClientRequest.setConnectionTimeout(timeout);
        return this;
    }

    public HttpClientBuilder setRequestTimeout(TimeoutDef timeout) {
        this.httpClientRequest.setRequestTimeout(timeout);
        return this;
    }
}
