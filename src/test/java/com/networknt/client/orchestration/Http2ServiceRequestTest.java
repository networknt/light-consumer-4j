package com.networknt.client.orchestration;

import com.networknt.client.model.HttpVerb;
import io.undertow.client.ClientResponse;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class Http2ServiceRequestTest {

    class DummyMarker {

    }

    Http2ServiceRequest underTest;
    Http2Client http2Client;
    final String path = "/dummy";
    ClientResponse clientResponse;
    Http2ServiceResponse http2ServiceResponse;
    Exception exception;
    RuntimeException runtimeException;
    CompletableFuture<ClientResponse> futureClientResponse;
    CompletableFuture<Http2ServiceResponse> futureHttp2ServiceResponse;
    DummyMarker typedObject = mock(DummyMarker.class);
    List<DummyMarker> typedList = mock(List.class);
    URI serviceURI;

    @Before
    public void setUp() throws Exception {
        this.serviceURI = mock(URI.class);
        this.http2Client = mock(Http2Client.class);
        this.futureClientResponse = spy(new CompletableFuture<>());
        this.futureHttp2ServiceResponse = spy(new CompletableFuture<>());
        this.clientResponse = mock(ClientResponse.class);
        this.http2ServiceResponse = mock(Http2ServiceResponse.class);
        this.exception = mock(Exception.class);
        this.runtimeException = mock(RuntimeException.class);
        doReturn(this.path).when(this.serviceURI).getPath();
        doReturn(this.futureClientResponse).when(this.http2Client).callService(eq(this.serviceURI),any(),any());
        this.underTest = new Http2ServiceRequest(this.serviceURI, HttpVerb.GET);
        this.underTest.http2Client = this.http2Client;
    }

    @Test
    public void call() {
        assertEquals(this.path, this.underTest.getClientRequest().getPath());
        int responseCode = 123;
        doReturn(responseCode).when(this.clientResponse).getResponseCode();
        String responseBody = "1234567890";
        doReturn(responseBody).when(this.clientResponse).getAttachment(any());
        CompletableFuture<Http2ServiceResponse> callFutureResponse = this.underTest.call();
        this.futureClientResponse.complete(this.clientResponse);
        verify(this.http2Client, times(1)).callService(eq(this.serviceURI), any(), eq(Optional.empty()));
        verify(this.futureClientResponse, times(1)).thenApplyAsync(any());
        assertEquals(responseCode, callFutureResponse.join().getClientResponseStatusCode());
        assertEquals(responseBody, callFutureResponse.join().getClientResponseBody());
    }

    @Test
    public void callWithSuccessfulCallback() {
        this.underTest = spy(this.underTest);
        doReturn(this.futureHttp2ServiceResponse).when(this.underTest).call();
        Consumer<Http2ServiceResponse> responseConsumer = mock(Consumer.class);
        Consumer<Exception> exceptionHandler = mock(Consumer.class);
        Http2ServiceRequest.CallWaiter callWaiter = this.underTest.call(responseConsumer, exceptionHandler);
        this.futureHttp2ServiceResponse.complete(this.http2ServiceResponse);
        callWaiter.waitForResponse();
        verify(this.underTest, times(1)).call();
        verify(this.futureHttp2ServiceResponse, times(1)).thenAcceptAsync(any());
        verify(responseConsumer, times(1)).accept(this.http2ServiceResponse);
        verify(exceptionHandler, times(0)).accept(any());
    }

    @Test
    public void callWithExceptionalPromiseHandler() {
        this.underTest = spy(this.underTest);
        doReturn(this.futureHttp2ServiceResponse).when(this.underTest).call();
        Consumer<Http2ServiceResponse> responseConsumer = mock(Consumer.class);
        Consumer<Exception> exceptionHandler = mock(Consumer.class);
        Http2ServiceRequest.CallWaiter callWaiter = this.underTest.call(responseConsumer, exceptionHandler);
        this.futureHttp2ServiceResponse.completeExceptionally(this.exception);
        callWaiter.waitForResponse();
        verify(this.underTest, times(1)).call();
        verify(this.futureHttp2ServiceResponse, times(1)).thenAcceptAsync(any());
        verify(responseConsumer, times(0)).accept(any());
        verify(exceptionHandler, times(1)).accept(any());
    }

    @Test
    public void callWithRuntimeExceptionHandler() {
        this.underTest = spy(this.underTest);
        doReturn(this.futureHttp2ServiceResponse).when(this.underTest).call();
        Consumer<Http2ServiceResponse> responseConsumer = mock(Consumer.class);
        doThrow(this.runtimeException).when(responseConsumer).accept(eq(this.http2ServiceResponse));
        Consumer<Exception> exceptionHandler = mock(Consumer.class);
        Http2ServiceRequest.CallWaiter callWaiter = this.underTest.call(responseConsumer, exceptionHandler);
        this.futureHttp2ServiceResponse.complete(this.http2ServiceResponse);
        callWaiter.waitForResponse();
        verify(this.underTest, times(1)).call();
        verify(this.futureHttp2ServiceResponse, times(1)).thenAcceptAsync(any());
        verify(responseConsumer, times(1)).accept(any());
        verify(exceptionHandler, times(1)).accept(this.runtimeException);
    }

    @Test
    public void callForTypedObject() throws Exception {
        this.underTest = spy(this.underTest);
        doReturn(this.futureHttp2ServiceResponse).when(this.underTest).call();
        doReturn(this.typedObject).when(this.http2ServiceResponse).getTypedClientResponse(eq(DummyMarker.class));

        CompletableFuture<DummyMarker> dummyMarkerCompletableFuture = this.underTest.callForTypedObject(DummyMarker.class);
        this.futureHttp2ServiceResponse.complete(this.http2ServiceResponse);
        assertEquals(this.typedObject, dummyMarkerCompletableFuture.join());
        verify(this.underTest, times(1)).call();
        verify(this.futureHttp2ServiceResponse, times(1)).thenComposeAsync(any());
        verify(this.http2ServiceResponse, times(1)).getTypedClientResponse(eq(DummyMarker.class));
    }

    @Test
    public void callForTypedObjectWithRuntimeException() throws Exception {
        this.underTest = spy(this.underTest);
        doReturn(this.futureHttp2ServiceResponse).when(this.underTest).call();
        doThrow(this.exception).when(this.underTest).optionallyValidateClientResponseStatusCode(anyInt());

        CompletableFuture<DummyMarker> dummyMarkerCompletableFuture = this.underTest.callForTypedObject(DummyMarker.class);
        this.futureHttp2ServiceResponse.complete(this.http2ServiceResponse);
        try {
            dummyMarkerCompletableFuture.join();
            fail();
        } catch (Exception e) {

        }
        verify(this.underTest, times(1)).call();
        verify(this.http2ServiceResponse, times(0)).getTypedClientResponse(any());
        verify(this.futureHttp2ServiceResponse, times(1)).thenComposeAsync(any());
    }

    @Test
    public void callForTypedObjectWithSuccessfulCallback() throws Exception {
        this.underTest = spy(this.underTest);
        doReturn(this.futureHttp2ServiceResponse).when(this.underTest).call();
        doReturn(this.typedObject).when(this.http2ServiceResponse).getTypedClientResponse(eq(DummyMarker.class));
        Consumer<DummyMarker> responseConsumer = mock(Consumer.class);
        Consumer<Exception> exceptionHandler = mock(Consumer.class);
        Http2ServiceRequest.CallWaiter callWaiter = this.underTest.callForTypedObject(DummyMarker.class,
                responseConsumer, exceptionHandler);
        this.futureHttp2ServiceResponse.complete(this.http2ServiceResponse);
        callWaiter.waitForResponse();
        verify(this.underTest, times(1)).call();
        verify(this.futureHttp2ServiceResponse, times(1)).thenAcceptAsync(any());
        verify(this.http2ServiceResponse, times(1)).getTypedClientResponse(eq(DummyMarker.class));
        verify(responseConsumer, times(1)).accept(this.typedObject);
        verify(exceptionHandler, times(0)).accept(any());
    }

    @Test
    public void callForTypedObjectWithExceptionalPromiseHandler() throws Exception {
        this.underTest = spy(this.underTest);
        doReturn(this.futureHttp2ServiceResponse).when(this.underTest).call();
        Consumer<DummyMarker> responseConsumer = mock(Consumer.class);
        Consumer<Exception> exceptionHandler = mock(Consumer.class);

        Http2ServiceRequest.CallWaiter callWaiter = this.underTest.callForTypedObject(DummyMarker.class,
                responseConsumer, exceptionHandler);

        this.futureHttp2ServiceResponse.completeExceptionally(this.exception);
        callWaiter.waitForResponse();
        verify(this.underTest, times(1)).call();
        verify(this.underTest, times(0)).optionallyValidateClientResponseStatusCode(anyInt());
        verify(this.http2ServiceResponse, times(0)).getTypedClientResponse(any());
        verify(this.futureHttp2ServiceResponse, times(0)).thenComposeAsync(any());
        verify(responseConsumer, times(0)).accept(any());
        verify(exceptionHandler, times(1)).accept(any());
    }

    @Test
    public void callForTypedObjectWithRuntimeExceptionHandler() throws Exception {
        this.underTest = spy(this.underTest);
        doReturn(this.futureHttp2ServiceResponse).when(this.underTest).call();
        doThrow(this.runtimeException).when(this.http2ServiceResponse).getTypedClientResponse(any());
        Consumer<DummyMarker> responseConsumer = mock(Consumer.class);
        Consumer<Exception> exceptionHandler = mock(Consumer.class);

        Http2ServiceRequest.CallWaiter callWaiter = this.underTest.callForTypedObject(DummyMarker.class,
                responseConsumer, exceptionHandler);

        this.futureHttp2ServiceResponse.complete(this.http2ServiceResponse);
        callWaiter.waitForResponse();
        verify(this.underTest, times(1)).call();
        verify(this.underTest, times(1)).optionallyValidateClientResponseStatusCode(anyInt());
        verify(this.http2ServiceResponse, times(1)).getTypedClientResponse(any());
        verify(this.futureHttp2ServiceResponse, times(1)).thenAcceptAsync(any());
        verify(responseConsumer, times(0)).accept(any());
        verify(exceptionHandler, times(1)).accept(eq(this.runtimeException));
    }

    @Test
    public void callForTypedList() {
    }

    @Test
    public void callForTypedList1() {
    }

    @Test
    public void optionallyValidateClientResponseStatusCode() {
        try {
            this.underTest.optionallyValidateClientResponseStatusCode(200);
        } catch (Exception e) {
            fail();
        }

        Predicate<Integer> statusValidator = mock(Predicate.class);
        this.underTest.setIsStatusCodeValid(statusValidator);
        doReturn(false).when(statusValidator).test(anyInt());
        try {
            this.underTest.optionallyValidateClientResponseStatusCode(200);
            fail();
        } catch (Exception e) {

        }

        doReturn(true).when(statusValidator).test(anyInt());
        try {
            this.underTest.optionallyValidateClientResponseStatusCode(200);
        } catch (Exception e) {
            fail();
        }
    }
}