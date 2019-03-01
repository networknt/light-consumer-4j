package com.networknt.client.orchestration;

import io.undertow.UndertowOptions;
import io.undertow.client.*;
import io.undertow.connector.ByteBufferPool;
import io.undertow.util.StringReadChannelListener;
import io.undertow.util.StringWriteChannelListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListeners;
import org.xnio.OptionMap;
import org.xnio.XnioWorker;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.ssl.XnioSsl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;

//this could should be merged to the exiting Http2Client class
//I cannot extend the class because everything is private in the exiting Http2Client class
//I cannot even compose/wrap the exiting class because everything is private in the exiting Http2Client class
//I had to copy paste some source code over for this partial implementation from the exiting Http2Client class
public class Http2Client {
    private final Map<String, ClientProvider> clientProviders;
    Logger logger = LoggerFactory.getLogger(Http2Client.class);
    public static Http2Client INSTANCE = new Http2Client() {};

    Http2Client() {
        ServiceLoader<ClientProvider> providers = ServiceLoader.load(ClientProvider.class, com.networknt.client.Http2Client.class.getClassLoader());
        final Map<String, ClientProvider> map = new HashMap<>();
        for (ClientProvider provider : providers) {
            for (String scheme : provider.handlesSchemes()) {
                map.put(scheme, provider);
            }
        }
        this.clientProviders = Collections.unmodifiableMap(map);
    }

    public CompletableFuture<ClientResponse> callService(URI uri, ClientRequest request, Optional<String> requestBody) {
        CompletableFuture<ClientConnection> futureConnection = this.connectAsync(uri);
        CompletableFuture<ClientResponse> futureClientResponse = futureConnection.thenComposeAsync(clientConnection -> {
            if (requestBody.isPresent()) {
                Http2Client.Http2ClientCompletableFutureWithRequest futureClientResponseWithRequest = new Http2ClientCompletableFutureWithRequest(requestBody.get());
                try {
                    clientConnection.sendRequest(request, futureClientResponseWithRequest);
                } catch (Exception e) {
                    futureClientResponseWithRequest.completeExceptionally(e);
                }
                return futureClientResponseWithRequest;
            } else {
                Http2Client.Http2ClientCompletableFutureNoRequest futureClientResponseNoRequest = new Http2ClientCompletableFutureNoRequest();
                try {
                    clientConnection.sendRequest(request, futureClientResponseNoRequest);
                } catch (Exception e) {
                    futureClientResponseNoRequest.completeExceptionally(e);
                }
                return futureClientResponseNoRequest;
            }
        });
        return futureClientResponse;
    }

    public CompletableFuture<ClientConnection> connectAsync(URI uri) {
        return this.connectAsync((InetSocketAddress) null, uri, com.networknt.client.Http2Client.WORKER, com.networknt.client.Http2Client.SSL, com.networknt.client.Http2Client.BUFFER_POOL,
                OptionMap.create(UndertowOptions.ENABLE_HTTP2, true));
    }

    public CompletableFuture<ClientConnection> connectAsync(InetSocketAddress bindAddress, final URI uri, final XnioWorker worker, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options) {
        CompletableFuture<ClientConnection> completableFuture = new CompletableFuture<>();
        ClientProvider provider = clientProviders.get(uri.getScheme());
        try {
            provider.connect(new ClientCallback<ClientConnection>() {
                @Override
                public void completed(ClientConnection r) {
                    completableFuture.complete(r);
                }

                @Override
                public void failed(IOException e) {
                    completableFuture.completeExceptionally(e);
                }
            }, bindAddress, uri, worker, ssl, bufferPool, options);
        } catch (Throwable t) {
            completableFuture.completeExceptionally(t);
        }
        return completableFuture;
    }

    public Http2ClientCompletableFutureNoRequest createCompletableFuture() {
        return new Http2ClientCompletableFutureNoRequest();
    }

    public Http2ClientCompletableFutureWithRequest createCompletableFuture(String requestBody) {
        return new Http2ClientCompletableFutureWithRequest(requestBody);
    }

    public class Http2ClientCompletableFutureNoRequest extends CompletableFuture<ClientResponse> implements ClientCallback<ClientExchange> {
        Logger logger = LoggerFactory.getLogger(Http2ClientCompletableFutureNoRequest.class);

        @Override
        public void completed(ClientExchange result) {
            result.setResponseListener(new ClientCallback<ClientExchange>() {
                @Override
                public void completed(final ClientExchange result) {
                    new StringReadChannelListener(result.getConnection().getBufferPool()) {

                        @Override
                        protected void stringDone(String string) {
                            result.getResponse().putAttachment(com.networknt.client.Http2Client.RESPONSE_BODY, string);
                            Http2ClientCompletableFutureNoRequest.super.complete(result.getResponse());
                        }

                        @Override
                        protected void error(IOException e) {
                            logger.error("IOException:", e);
                            Http2ClientCompletableFutureNoRequest.super.completeExceptionally(e);
                        }
                    }.setup(result.getResponseChannel());
                }

                @Override
                public void failed(IOException e) {
                    logger.error("IOException:", e);
                    Http2ClientCompletableFutureNoRequest.super.completeExceptionally(e);
                }
            });
            try {
                result.getRequestChannel().shutdownWrites();
                if(!result.getRequestChannel().flush()) {
                    result.getRequestChannel().getWriteSetter().set(ChannelListeners.<StreamSinkChannel>flushingChannelListener(null, null));
                    result.getRequestChannel().resumeWrites();
                }
            } catch (IOException e) {
                logger.error("IOException:", e);
                Http2ClientCompletableFutureNoRequest.super.completeExceptionally(e);
            }
        }

        @Override
        public void failed(IOException e) {
            logger.error("IOException:", e);
            Http2ClientCompletableFutureNoRequest.super.completeExceptionally(e);
        }

    }


    public class Http2ClientCompletableFutureWithRequest extends CompletableFuture<ClientResponse> implements ClientCallback<ClientExchange> {
        Logger logger = LoggerFactory.getLogger(Http2ClientCompletableFutureWithRequest.class);

        String requestBody;
        public Http2ClientCompletableFutureWithRequest(String requestBody) {
            this.requestBody = requestBody;
        }

        @Override
        public void completed(ClientExchange result) {
            new StringWriteChannelListener(requestBody).setup(result.getRequestChannel());
            result.setResponseListener(new ClientCallback<ClientExchange>() {
                @Override
                public void completed(ClientExchange result) {
                    new StringReadChannelListener(com.networknt.client.Http2Client.BUFFER_POOL) {
                        @Override
                        protected void stringDone(String string) {
                            result.getResponse().putAttachment(com.networknt.client.Http2Client.RESPONSE_BODY, string);
                            Http2ClientCompletableFutureWithRequest.super.complete(result.getResponse());
                        }

                        @Override
                        protected void error(IOException e) {
                            logger.error("IOException:", e);
                            Http2ClientCompletableFutureWithRequest.super.completeExceptionally(e);
                        }
                    }.setup(result.getResponseChannel());
                }

                @Override
                public void failed(IOException e) {
                    logger.error("IOException:", e);
                    Http2ClientCompletableFutureWithRequest.super.completeExceptionally(e);
                }
            });
        }

        @Override
        public void failed(IOException e) {
            logger.error("IOException:", e);
            Http2ClientCompletableFutureWithRequest.super.completeExceptionally(e);
        }

    }
}
