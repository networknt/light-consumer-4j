package com.networknt.client.orchestration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.Config;
import io.undertow.client.ClientRequest;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Http2ServiceRequest {

    public enum HttpVerb {
        GET(Methods.GET),
        POST(Methods.POST),
        PUT(Methods.PUT),
        DELETE(Methods.DELETE),
        HEAD(Methods.HEAD),
        OPTIONS(Methods.OPTIONS),
        TRACE(Methods.TRACE),
        PATCH(Methods.PATCH),
        CONNECT(Methods.CONNECT);

        public final HttpString verbHttpString;

        HttpVerb(HttpString verb) {
            this.verbHttpString = verb;
        }
    }

    public enum CommonHttpHeader {
        ACCEPT(Headers.ACCEPT, "application/json");

        final HttpString headerName;
        final String headerValue;

        CommonHttpHeader(HttpString headerName, String headerValue) {
            this.headerName = headerName;
            this.headerValue = headerValue;
        }
    }

    final URI hostURI;
    Optional<String> requestBody = Optional.empty();

    final ClientRequest clientRequest;

    Http2Client http2Client = Http2Client.INSTANCE;
    ObjectMapper objectMapper = Config.getInstance().getMapper();

    Optional<Predicate<Integer>> isStatusCodeValid = Optional.empty();

    public Http2ServiceRequest(URI uri, HttpVerb verb) {
        this.hostURI = uri;
        this.clientRequest = new ClientRequest().setMethod(verb.verbHttpString).setPath(uri.getPath());
    }

    public Http2ServiceRequest(URI uri, String path, HttpVerb verb) {
        this.hostURI = uri;
        this.clientRequest = new ClientRequest().setMethod(verb.verbHttpString).setPath(path);
    }

    public void addRequestHeader(CommonHttpHeader header) {
        this.clientRequest.getRequestHeaders().add(header.headerName, header.headerValue);
    }

    public void addRequestHeader(String headerName, String headerValue) {
        this.clientRequest.getRequestHeaders().put(new HttpString(headerName), headerValue);
    }

    public void addRequestHeader(String headerName, int headerValue) {
        this.clientRequest.getRequestHeaders().put(new HttpString(headerName), headerValue);
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = Optional.ofNullable(requestBody);
    }

    public void setRequestBody(Object requestBody) throws Exception {
        this.requestBody = Optional.ofNullable(this.objectMapper.writeValueAsString(requestBody));
    }

    public void setIsStatusCodeValid(Predicate<Integer> isStatusCodeValid) {
        this.isStatusCodeValid = Optional.of(isStatusCodeValid);
    }

    public CompletableFuture<Http2ServiceResponse> call() {
        return http2Client.callService(hostURI, clientRequest, requestBody).thenApplyAsync(
                response -> new Http2ServiceResponse(response));
    }

    public CallWaiter call(Consumer<Http2ServiceResponse> callback, Consumer<Exception> exceptionHandler) {
        return new CallWaiter(this.call().thenAcceptAsync(http2ServiceResponse -> {
            try {
                callback.accept(http2ServiceResponse);
            } catch (Exception e) {
                exceptionHandler.accept(e);
            }
        }), exceptionHandler);
    }

    public void optionallyValidateClientResponseStatusCode(int statusCode) throws Exception {
        if (this.isStatusCodeValid.isPresent()) {
            if (!this.isStatusCodeValid.get().test(statusCode)) {
                throw new Exception("cannot type the response object because response code is " + statusCode);
            }
        }
    }

    public <ResponseType> CompletableFuture<ResponseType> callForTypedObject(Class<ResponseType> responseTypeClass) {
        return this.call().thenComposeAsync(http2ServiceResponse -> {
            CompletableFuture<ResponseType> completableFuture = new CompletableFuture<>();
            try {
                optionallyValidateClientResponseStatusCode(http2ServiceResponse.getClientResponseStatusCode());
                completableFuture.complete(http2ServiceResponse.getTypedClientResponse(responseTypeClass));
            } catch (Exception e) {
                completableFuture.completeExceptionally(e);
            }
            return completableFuture;
        });
    }

    public <ResponseType> CallWaiter callForTypedObject(Class<ResponseType> responseTypeClass, Consumer<ResponseType> callback, Consumer<Exception> exceptionHandler) {
        return new CallWaiter(this.call().thenAcceptAsync(http2ServiceResponse -> {
            try {
                optionallyValidateClientResponseStatusCode(http2ServiceResponse.getClientResponseStatusCode());
                callback.accept(http2ServiceResponse.getTypedClientResponse(responseTypeClass));
            } catch (Exception e) {
                exceptionHandler.accept(e);
            }
        }), exceptionHandler);
    }

    public <ResponseType> CompletableFuture<List<ResponseType>> callForTypedList(Class<ResponseType> responseTypeClass) {
        return this.call().thenComposeAsync(http2ServiceResponse -> {
            CompletableFuture<List<ResponseType>> completableFuture = new CompletableFuture<>();
            try {
                optionallyValidateClientResponseStatusCode(http2ServiceResponse.getClientResponseStatusCode());
                completableFuture.complete(http2ServiceResponse.getTypedListClientResponse(responseTypeClass));
            } catch (Exception e) {
                completableFuture.completeExceptionally(e);
            }
            return completableFuture;
        });
    }

    public <ResponseType> CallWaiter callForTypedList(Class<ResponseType> responseTypeClass, Consumer<List<ResponseType>> callback, Consumer<Exception> exceptionHandler) {
        return new CallWaiter(this.call().thenAcceptAsync(http2ServiceResponse -> {
            try {
                optionallyValidateClientResponseStatusCode(http2ServiceResponse.getClientResponseStatusCode());
                callback.accept(http2ServiceResponse.getTypedListClientResponse(responseTypeClass));
            } catch (Exception e) {
                exceptionHandler.accept(e);
            }
        }), exceptionHandler);
    }

    public static class CallWaiter {
        private final CompletableFuture<Void> future;
        private final Consumer<Exception> exceptionHandler;

        public CallWaiter(CompletableFuture<Void> future, Consumer<Exception> exceptionHandler) {
            this.future = future;
            this.exceptionHandler = exceptionHandler;
        }

        public void waitForResponse() {
            try {
                future.get();
            } catch (Exception e) {
                this.exceptionHandler.accept(e);
            }
        }
    }

}
