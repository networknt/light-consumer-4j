package com.networknt.client.builder;

import com.networknt.client.Http2Client;
import com.networknt.client.model.ServiceDef;
import com.networknt.exception.ApiException;
import com.networknt.exception.ClientException;
import com.networknt.registry.Registry;
import com.networknt.registry.URLImpl;
import com.networknt.client.model.ConsumerConfig;
import com.networknt.cluster.Cluster;
import com.networknt.config.Config;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.util.Headers;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
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
    ConsumerConfig consumerConfig;
    @Mock
    Config config;
    @Mock
    ConnectionCacheManager connectionCacheManager;
    @Mock
    HttpClientRequest httpClientRequest;
    @Mock
    ClientConnection clientConnection;
    @Mock
    ServiceDef serviceDef;

    static Cluster cluster = Mockito.mock(Cluster.class);
    static Http2Client http2Client = Mockito.mock(Http2Client.class);

    URI uri;

    @Before
    public void setUp1() throws Exception {
        PowerMockito.mockStatic(SingletonServiceFactory.class);
        PowerMockito.when(SingletonServiceFactory.getBean(Cluster.class)).thenReturn(cluster);
        PowerMockito.whenNew(ConnectionCacheManager.class).withNoArguments().thenReturn(connectionCacheManager);
        PowerMockito.whenNew(HttpClientRequest.class).withNoArguments().thenReturn(httpClientRequest);

        PowerMockito.mockStatic(Http2Client.class);
        PowerMockito.when(Http2Client.getInstance()).thenReturn(http2Client);

        PowerMockito.when(httpClientRequest.getConnectionCacheTTLms()).thenReturn(10000l);
        PowerMockito.when(httpClientRequest.getHttp2Enabled()).thenReturn(true);
        PowerMockito.when(httpClientRequest.getMaxReqCount()).thenReturn(-1);
        when(httpClientRequest.getApiHost()).thenReturn("https://localhost:8080");
        uri = new URI("https://localhost:8080");
        when(connectionCacheManager.getConnection(uri, 10000, null, true, -1,60000)).thenReturn(clientConnection);
    }

    //@Test
    public void testHttpClientBuilderAuthToken() throws Exception {
        ClientRequest clientRequest = new ClientRequest();
        PowerMockito.when(httpClientRequest.getClientRequest()).thenReturn(clientRequest);
        PowerMockito.when(httpClientRequest.getAddCCToken()).thenReturn(false);
        PowerMockito.when(httpClientRequest.getAuthToken()).thenReturn("12345abc");
        HttpClientBuilder cb = new HttpClientBuilder();
        cb.send();
        verify(http2Client, times(1)).addAuthToken(anyObject(), anyString());
        verify(http2Client, never()).addCcToken(clientRequest);
        Assert.assertEquals("12345abc", cb.getAuthToken());
    }

    //@Test
    public void testHttpClientBuilderCCToken() throws Exception {
        ClientRequest clientRequest = new ClientRequest();
        PowerMockito.when(httpClientRequest.getClientRequest()).thenReturn(clientRequest);
        PowerMockito.when(httpClientRequest.getAddCCToken()).thenReturn(true);
        PowerMockito.when(httpClientRequest.getAuthToken()).thenReturn(null);
        HttpClientBuilder cb = new HttpClientBuilder();
        cb.send();
        verify(http2Client, never()).addAuthToken(any(), eq("321321"));
        verify(http2Client, times(1)).addCcToken(any());
    }

    //@Test
    public void testHttpClientBuilderApiHost() throws Exception {
        ClientRequest clientRequest = new ClientRequest();
        clientRequest.getRequestHeaders().put(Headers.HOST, "localhost");
        PowerMockito.when(httpClientRequest.getClientRequest()).thenReturn(clientRequest);
        HttpClientBuilder cb = new HttpClientBuilder();
        cb.send();
        verify(httpClientRequest, times(1)).getApiHost();
        Assert.assertEquals("https://localhost:8080", httpClientRequest.getApiHost());
    }

    //@Test
    public void testHttpClientBuilderHost() throws Exception {
        ClientRequest clientRequest = new ClientRequest();
        PowerMockito.when(httpClientRequest.getClientRequest()).thenReturn(clientRequest);
        HttpClientBuilder cb = new HttpClientBuilder();
        cb.send();
        Assert.assertEquals("localhost", uri.getHost());
    }

    //@Test
    public void testHttpClientBuilderEnvConfig() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("com.networknt.hello-3", "dev2");
        when(consumerConfig.getServiceEnv()).thenReturn(map);
        PowerMockito.mockStatic(Config.class);
        PowerMockito.when(Config.getInstance()).thenReturn(config);
        PowerMockito.when(config.getJsonObjectConfig("consumer", ConsumerConfig.class)).thenReturn(consumerConfig);
        PowerMockito.when(serviceDef.getServiceId()).thenReturn("com.networknt.hello-3");
        PowerMockito.when(httpClientRequest.getServiceDef()).thenReturn(serviceDef);
        HttpClientBuilder cb = new HttpClientBuilder();
        cb.setServiceDef(serviceDef);
        Assert.assertEquals("dev2", consumerConfig.getServiceEnv().get(serviceDef.getServiceId()));

    }

    //@Test
    public void testHttpClientBuilderServiceUrl() throws Exception {
        PowerMockito.when(httpClientRequest.getServiceDef()).thenReturn(serviceDef);
        PowerMockito.when(serviceDef.getEnvironment()).thenReturn("dev");
        PowerMockito.when(cluster.serviceToUrl(any(), any(), any(), any())).thenReturn("https://localhost:8888");
        HttpClientBuilder cb = new HttpClientBuilder();
        cb.setServiceDef(serviceDef);
        Assert.assertEquals("https://localhost:8888", cluster.serviceToUrl(any(), any(), any(), any()));
    }
}
