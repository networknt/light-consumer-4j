package com.networknt.client.builder;

import com.networknt.client.Http2Client;
import com.networknt.client.model.ConsumerConfig;
import com.networknt.cluster.Cluster;
import com.networknt.config.Config;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.util.Headers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Http2Client.class, ConnectionCacheManager.class,
        HttpClientBuilder.class, HttpClientRequest.class, SingletonServiceFactory.class,
        ConsumerConfig.class, Config.class, ServiceDef.class, Cluster.class})
@PowerMockIgnore("javax.net.ssl.*")
public class TestHttpClientBuilder {

    @Mock
    Cluster cluster;
    @Mock
    ConsumerConfig consumerConfig;
    @Mock
    Config config;
    @Mock
    Http2Client http2Client;
    @Mock
    ConnectionCacheManager connectionCacheManager;
    @Mock
    HttpClientRequest httpClientRequest;
    @Mock
    ClientConnection clientConnection;
    @Mock
    ServiceDef serviceDef;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(SingletonServiceFactory.class);
        PowerMockito.when(SingletonServiceFactory.getBean(Cluster.class)).thenReturn(cluster);
        PowerMockito.mockStatic(Http2Client.class);
        PowerMockito.when(Http2Client.getInstance()).thenReturn(http2Client);
        PowerMockito.whenNew(ConnectionCacheManager.class).withNoArguments().thenReturn(connectionCacheManager);
        PowerMockito.whenNew(HttpClientRequest.class).withNoArguments().thenReturn(httpClientRequest);

        PowerMockito.when(httpClientRequest.getConnectionCacheTTLms()).thenReturn(10000l);
        PowerMockito.when(httpClientRequest.getHttp2Enabled()).thenReturn(true);
        PowerMockito.when(httpClientRequest.getMaxReqCount()).thenReturn(-1);
        when(httpClientRequest.getApiHost()).thenReturn("https://localhost:8080");
        URI uri = new URI("https://localhost:8080");
        when(connectionCacheManager.getConnection(uri, 10000, null, true, -1)).thenReturn(clientConnection);
    }

    @Test
    public void testHttpClientBuilderAuthToken() throws Exception {
        ClientRequest clientRequest = new ClientRequest();
        PowerMockito.when(httpClientRequest.getClientRequest()).thenReturn(clientRequest);
        PowerMockito.when(httpClientRequest.getAddCCToken()).thenReturn(false);
        PowerMockito.when(httpClientRequest.getAuthToken()).thenReturn("12345abc");
        HttpClientBuilder cb = new HttpClientBuilder();
        cb.send();
        verify(http2Client, times(1)).addAuthToken(any(), eq("12345abc"));
        verify(http2Client, never()).addCcToken(any());
        Assert.assertEquals("12345abc", cb.getAuthToken());
    }

    @Test
    public void testHttpClientBuilderCCToken() throws Exception {
        ClientRequest clientRequest = new ClientRequest();
        PowerMockito.when(httpClientRequest.getClientRequest()).thenReturn(clientRequest);
        PowerMockito.when(httpClientRequest.getAddCCToken()).thenReturn(true);
        PowerMockito.when(httpClientRequest.getAuthToken()).thenReturn(null);
        HttpClientBuilder cb = new HttpClientBuilder();
        cb.send();
        verify(http2Client, never()).addAuthToken(any(), eq("12345abc"));
        verify(http2Client, times(1)).addCcToken(any());
    }

    @Test
    public void testHttpClientBuilderHost() throws Exception {
        ClientRequest clientRequest = new ClientRequest();
        clientRequest.getRequestHeaders().put(Headers.HOST, "localhost");
        PowerMockito.when(httpClientRequest.getClientRequest()).thenReturn(clientRequest);
        HttpClientBuilder cb = new HttpClientBuilder();
        cb.send();
        verify(httpClientRequest, times(4)).getClientRequest();
        Assert.assertEquals("localhost", httpClientRequest.getClientRequest().getRequestHeaders().get(Headers.HOST).getFirst());
    }

    @Test
    public void testHttpClientBuilderWithoutHost() throws Exception {
        ClientRequest clientRequest = new ClientRequest();
        PowerMockito.when(httpClientRequest.getClientRequest()).thenReturn(clientRequest);
        HttpClientBuilder cb = new HttpClientBuilder();
        cb.send();
        verify(httpClientRequest, times(3)).getClientRequest();
    }

    @Test
    public void testHttpClientBuilderWithoutEnvConfig() throws Exception {
        PowerMockito.when(serviceDef.getEnvironment()).thenReturn("dev");
        PowerMockito.when(cluster.serviceToUrl(anyString(), anyString(), anyString(), anyString())).thenReturn("https://localhost:8080");
        PowerMockito.when(httpClientRequest.getServiceDef()).thenReturn(serviceDef);
        HttpClientBuilder cb = new HttpClientBuilder();
        cb.setServiceDef(serviceDef);
        verify(serviceDef, times(2)).getEnvironment();
        verify(httpClientRequest, times(4)).getServiceDef();
    }

    @Test
    public void testHttpClientBuilderWithEnvConfig() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("com.networknt.hello-1", "dev1");
        when(consumerConfig.getServiceEnv()).thenReturn(map);
        PowerMockito.mockStatic(Config.class);
        PowerMockito.when(Config.getInstance()).thenReturn(config);
        PowerMockito.when(config.getJsonObjectConfig("consumer", ConsumerConfig.class)).thenReturn(consumerConfig);

        PowerMockito.when(serviceDef.getServiceId()).thenReturn("com.networknt.hello-1");
        PowerMockito.when(cluster.serviceToUrl(anyString(), anyString(), anyString(), anyString())).thenReturn("https://localhost:8080");
        PowerMockito.when(httpClientRequest.getServiceDef()).thenReturn(serviceDef);
        HttpClientBuilder cb = new HttpClientBuilder();
        cb.setServiceDef(serviceDef);
        verify(serviceDef, times(2)).getEnvironment();
    }

    @Test
    public void testHttpClientBuilderServiceUrl() throws Exception {
        PowerMockito.when(httpClientRequest.getServiceDef()).thenReturn(serviceDef);
        PowerMockito.when(serviceDef.getProtocol()).thenReturn("https");
        PowerMockito.when(serviceDef.getServiceId()).thenReturn("com.networknt.hello-1");
        PowerMockito.when(serviceDef.getEnvironment()).thenReturn("dev");
        PowerMockito.when(serviceDef.getRequestKey()).thenReturn("dev");
        PowerMockito.when(cluster.serviceToUrl("https", "com.networknt.hello-1", "dev", "dev")).thenReturn("https://localhost:8080");

        HttpClientBuilder cb = new HttpClientBuilder();
        cb.setServiceDef(serviceDef);
        verify(serviceDef, times(2)).getEnvironment();
        verify(httpClientRequest, times(4)).getServiceDef();

    }

}



