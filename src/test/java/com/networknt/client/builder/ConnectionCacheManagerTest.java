package com.networknt.client.builder;

import com.networknt.client.model.TimeoutDef;
import com.networknt.client.orchestration.RequestStressTest;
import com.networknt.petstore.handler.TestServer;
import io.undertow.client.ClientConnection;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.TimeUnit;

public class ConnectionCacheManagerTest {
    @ClassRule
    public static TestServer server = TestServer.getInstance();

    static final Logger logger = LoggerFactory.getLogger(RequestStressTest.class);
    static final boolean enableHttp2 = server.getServerConfig().isEnableHttp2();
    static final boolean enableHttps = server.getServerConfig().isEnableHttps();
    static final int httpPort = server.getServerConfig().getHttpPort();
    static final int httpsPort = server.getServerConfig().getHttpsPort();
    static final String url = enableHttp2 || enableHttps ? "https://localhost:" + httpsPort : "http://localhost:" + httpPort;
    static final URI uri = URI.create(url);
    static final ConnectionCacheManager manager = new ConnectionCacheManager();

    /**
     * Before run this test, you need to update the CacheableConnection to set the parkedTtl to 100;
     * @throws Exception
     */
    //@Test
    public void testCacheableConnection() throws Exception {
        ClientConnection connection0 = manager.getConnection(uri, 2000, new TimeoutDef(2, TimeUnit.SECONDS), true, 1000,60000);
        ClientConnection connection1 = manager.getConnection(uri, 2000, new TimeoutDef(2, TimeUnit.SECONDS), true, 1000,60000);
        Assert.assertTrue(connection0 == connection1); // Both connections should be the same instance.
        Thread.sleep(3000);
        ClientConnection connection2 = manager.getConnection(uri, 2000, new TimeoutDef(2, TimeUnit.SECONDS), true, 1000,60000);
        Assert.assertFalse(connection1 == connection2); // The connection 1 is expired and moved to the parked map. connection2 is another instance.
        Thread.sleep(3000);
        ClientConnection connection3 = manager.getConnection(uri, 2000, new TimeoutDef(2, TimeUnit.SECONDS), true, 1000,60000);
        Assert.assertFalse(connection1.isOpen()); // at this moment, the connection1 is closed if parked ttl is set to 100 milliseconds for debugging.
        Assert.assertTrue(connection2.isOpen());
        Assert.assertTrue(connection3.isOpen());
        Thread.sleep(3000);
        Assert.assertTrue(connection2.isOpen());

    }

}
