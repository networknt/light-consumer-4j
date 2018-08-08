package com.networknt.client.builder;

import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

public class ConnectionCacheManager {
    private static Logger logger = LoggerFactory.getLogger(ConnectionCacheManager.class);
    private static ExecutorService executorService = Executors.newFixedThreadPool(2);
    private static Map<String, CacheableConnection> clientConnectionMap = new HashMap<>();

    private static OptionMap http2OptionMap = OptionMap.create(UndertowOptions.ENABLE_HTTP2, true);
    private static OptionMap http1OptionMap = OptionMap.EMPTY;

    /**
     * Entry point to fetch a connection.
     *
     * @param hostUri The protocol/host/{port} to connect to.
     * @param connectionTTL How long a connection will remain available if a new one needs to be made.
     * @param connectionRequestTimeout How long to wait for a connection to be established.
     * @param isHttp2Enabled Whether an http2 or http1 connection will be made.
     * @return A connection to the host.
     */
    public ClientConnection getConnection(URI hostUri, long connectionTTL, TimeoutDef connectionRequestTimeout, boolean isHttp2Enabled) throws InterruptedException, ExecutionException, TimeoutException {
        CacheableConnection connection = clientConnectionMap.get(hostUri.toString());
        if (connection != null && connection.isOpen()) {
            logger.debug("Reusing open connection to: " + hostUri);
            return connection.getCachedConnection();
        }
        logger.debug("Creating a new connection to: " + hostUri);
        CacheableConnection cacheableConnection = new CacheableConnection(this.connect(hostUri, connectionRequestTimeout, isHttp2Enabled), connectionTTL);
        clientConnectionMap.put(hostUri.toString(), cacheableConnection);
        return cacheableConnection.getCachedConnection();
    }

    /**
     * Get the connection before issuing the request. This method will spawn a thread within the cached thread pool
     * for waiting before timing out.
     *
     * @return The connection.
     * @throws TimeoutException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private ClientConnection connect(URI host, TimeoutDef connectionRequestTimeout, boolean isHttp2Enabled) throws TimeoutException, ExecutionException, InterruptedException {
        OptionMap options = isHttp2Enabled ? http2OptionMap : http1OptionMap;
        Future<ClientConnection> connectionFuture = executorService.submit(new ClientConnectionCallable(host, options));
        try {
            return connectionFuture.get(connectionRequestTimeout.getTimeout(), connectionRequestTimeout.getUnit());
        } catch (TimeoutException e) {
            logger.error("Timeout occurred when getting connection to: " + host, e);
            throw e;
        } catch (Exception e) {
            logger.error("Error occurred when getting connection.", e);
            throw e;
        }
    }


}
