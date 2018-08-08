package com.networknt.client.builder;

import com.networknt.client.Http2Client;
import io.undertow.client.ClientConnection;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.concurrent.Callable;

public class ClientConnectionCallable implements Callable<ClientConnection> {

    private static Http2Client client = Http2Client.getInstance();

    private URI host;
    private OptionMap options;

    public ClientConnectionCallable(URI host, OptionMap options) {
        this.host = host;
        this.options = options;
    }

    @Override
    public ClientConnection call() throws Exception {
        return client.connect(this.host, Http2Client.WORKER, Http2Client.SSL,
                Http2Client.POOL, this.options).get();
    }
}
