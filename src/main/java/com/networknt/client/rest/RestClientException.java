package com.networknt.client.rest;

import com.networknt.exception.ClientException;

public class RestClientException extends ClientException {

    /**
     * Construct a new instance of {@code RestClientException} with the given message.
     * @param msg the message
     */
    public RestClientException(String msg) {
        super(msg);
    }

    /**
     * Construct a new instance of {@code RestClientException} with the given message and
     * exception.
     * @param msg the message
     * @param ex the exception
     */
    public RestClientException(String msg, Throwable ex) {
        super(msg, ex);
    }
}
