package com.networknt.client.rest;

import com.networknt.client.builder.TimeoutDef;
import org.xnio.Option;

/**
 * RestClientOptions to configure the rest client. These are directly analogous to configurable properties in HTTPClientBuilder
 */
public class RestClientOptions {
    public static final Option<Boolean> DISABLE_HTTP2 = Option.simple(RestClientOptions.class, "DISABLE_HTTP2", Boolean.class);
    public static final Option<Boolean> ADD_CC_TOKEN = Option.simple(RestClientOptions.class, "ADD_CC_TOKEN", Boolean.class);
    public static final Option<Long> CONN_CACHE_TTL = Option.simple(RestClientOptions.class, "CONN_CACHE_TTL", Long.class);
    public static final Option<TimeoutDef> CONN_REQ_TIMEOUT = Option.simple(RestClientOptions.class, "CONN_REQ_TIMEOUT", TimeoutDef.class);
    public static final Option<TimeoutDef> REQ_TIMEOUT = Option.simple(RestClientOptions.class, "CONN_REQ_TIMEOUT", TimeoutDef.class);
    public static final Option<Integer> MAX_REQ_CNT = Option.simple(RestClientOptions.class, "MAX_REQ_CNT", Integer.class);
}