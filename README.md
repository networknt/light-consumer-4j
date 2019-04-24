# Light Consumer 4j

Light Consumer 4j is module which helps for consumers to easily integrate with light-4j apis.
It is built on top of Http2Client & java8,it has a lot of extra features like connection pooling etc.
it supports both direct URL and service discover (Consul) to call the apis.


### Usage
Add dependency in your project.

```xml
<dependency>
    <groupId>com.networknt</groupId>
    <artifactId>light-consumer-4j</artifactId>
    <version>1.5.29</version>
</dependency>
```

### Configuration

Setup the client and registry on the service.yml config file (or on centralized config file values.yml).

For example, if you are using consul client &  registry:


```yaml

- com.networknt.registry.URL:
  - com.networknt.registry.URLImpl:
      parameters:
        registryRetryPeriod: '30000'
- com.networknt.consul.client.ConsulClient:
  - com.networknt.consul.client.ConsulClientImpl
- com.networknt.registry.Registry:
  - com.networknt.consul.ConsulRegistry
- com.networknt.balance.LoadBalance:
  - com.networknt.balance.RoundRobinLoadBalance
- com.networknt.cluster.Cluster:
  - com.networknt.cluster.LightCluster


```

There is a config file for light-consumer-4j library which used to set service environment tag.

-- consumer.yml

Environment tag that will be registered on consul to support multiple instances per env for testing.

This config file (consumer.yml) is optional. If user doesn't set this file (or the values on the values.yml), system will use default value (null) for service environment tag



### Implementation detail:


####  ClientBuilder

Here are the methods available in ClientBuilder.

 Future <ClientResponse> send()           // Send request.
 
 String getServiceUrl()                   // Get the resolved serviceUrl
 
 disableHttp2()                           // Disable Http2, by default it is enabled.
 
 setClientRequest(ClientRequest reg)      // set client request to httpClientRequest.
 
 setRequestBody(String requestBody)       // set requestBody to httpClientRequest.
 
 setHeaderValue(HttpString headerName,..) // set headerName and headerValue to httpClientRequest.
 
 setApiHost(String apiUrl)	              // set apiUrl into httpClientRequest to call api with direct url .
 
 setConnectionCacheTTLms(long tTLms)      // set connection cache time out.
 
 setRequestTimeout(TimeoutDef timeout)    // set request time out
 
 setConnectionRequestTimeout(TimeoutDef t)// set connection request timeout
 
 setAuthToken(String authToken)           // Set the auth token in the request
 
 getAuthToken()                           // get authToken 
 
 addCCToken()                             // add the access token in request header and also checks access token expiration. 
 
 setServiceDef(ServiceDef serviceDef) // set protocol,service id , environment  and requestKey to call api via consul

 setMaxReqCount(int maxReqCount)          // enable connection cache by request number


 ####  LightRestClient

  LightRestClient provide general Restful method call by user provided URL or service Id.

  -- Get

  -- POST

  -- PUT

  --DELETE


 ### Example service call:
 
 #### Code example to call the api by using HttpClientBuilder.
 
Call the api with direct URL:

```
Future<ClientResponse> clientRequest = new HttpClientBuilder()
                      
		    //set direct url
                    .setApiHost("https://localhost:8453")
					
                    .setClientRequest(new ClientRequest().setPath("/v1/customers/1").setMethod(Methods.GET))
                    .setLatch(new CountDownLatch(1))
                    .setAuthToken("")
                    .disableHttp2()
                    .addCCToken()
                    .setConnectionRequestTimeout(new TimeoutDef(100, TimeUnit.SECONDS))
                    .setRequestTimeout(new TimeoutDef(100, TimeUnit.SECONDS))
                    .setRequestBody("")
                    .setConnectionCacheTTLms(10000)
                    .setMaxReqCount(5)
                    .send();
            ClientResponse clientResponse = clientRequest.get();
```


Call the api via service discover (Consul):

```
Future<ClientResponse> clientRequest = new HttpClientBuilder()
                     
		    //set protocol,service id , environment  and requestKey 
                    .setServiceDef(new ServiceDef("https", "training.customers-1.00.00","training", null))
					
                    .setClientRequest(new ClientRequest().setPath("/v1/customers/1").setMethod(Methods.GET))
                    .setLatch(new CountDownLatch(1))
	            .setAuthToken("")
                    .disableHttp2()
                    .addCCToken()
                    .setConnectionRequestTimeout(new TimeoutDef(100, TimeUnit.SECONDS))
                    .setRequestTimeout(new TimeoutDef(100, TimeUnit.SECONDS))
                    .setRequestBody("")
                    .setConnectionCacheTTLms(10000)
                    .setMaxReqCount(5)
                    .send();
            ClientResponse clientResponse = clientRequest.get();
```



 #### Code example to call the api by using LightRestClient.


Call the api with direct URL:

```
        LightRestClient lightRestClient = new LightRestClient();
        String requestStr = "{\"selection\":{\"accessCard\":\"22222222\",\"selectID\":10009,\"crossReference\":{\"externalSystemID\":226,\"referenceType\":2,\"ID\":\"122222\"}}}";
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Content-Type", "application/json");
        headerMap.put("Transfer-Encoding", "chunked");

       Map resultMap = lightRestClient.post("https://localhost:8467", "/networknt/select/",  Map.class, headerMap, requestStr);

 ```


 Call the api with docker image name (service running by docker-compose in the docker container):

 ```
         LightRestClient lightRestClient = new LightRestClient();
         String requestStr = "{\"selection\":{\"accessCard\":\"22222222\",\"selectID\":10009,\"crossReference\":{\"externalSystemID\":226,\"referenceType\":2,\"ID\":\"122222\"}}}";
         Map<String, String> headerMap = new HashMap<>();
         headerMap.put("Content-Type", "application/json");
         headerMap.put("Transfer-Encoding", "chunked");

        Map resultMap = lightRestClient.post("https://apia-service:8467", "/networknt/select/",  Map.class, headerMap, requestStr);


  ```


  Call the api with service discover (Consul):

 ```
        LightRestClient lightRestClient = new LightRestClient();
        String requestStr = "{\"selection\":{\"accessCard\":\"22222222\",\"selectID\":10009,\"crossReference\":{\"externalSystemID\":226,\"referenceType\":2,\"ID\":\"122222\"}}}";
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Content-Type", "application/json");
        headerMap.put("Transfer-Encoding", "chunked");

        ServiceDef serviceDef = new ServiceDef("https", "com.networknt.apia.selection-1.00.00", null);
        Map resultMap =  lightRestClient.post(serviceDef, "/networknt/select/", Map.class, headerMap, requestStr);


  ```



 ### ParallelRestClient

 Use java 8 CompletableFuture to handle two or more threads asynchronous computation; We can send requests parallel and process result asynchronously.

 It can better system performance (coroutines, no threads):

 	-- More responsiveness (no blocking on threads)

 	-- More throughput (only bound by CPU)

 #### Code example use ParallelRestClient call

 ServiceDef serviceDef = new ServiceDef(“”https, “com.networknt.petstore1”, null, null);

 Http2ServiceRequest request1 = Http2ServiceRequest(serviceDef, “/get”, HttpVerb.valueOf(“GET”);
 Http2ServiceRequest request2 = Http2ServiceRequest(serviceDef, “/getById/1” “GET”);

 Collection<CompletableFuture<?>> completableFutures = new HashSet<>();
 CompletableFuture<Map> futureResponse1 = request1.callForTypedObject(Map.class);
 CompletableFuture<Map> futureResponse2 = request2.callForTypedObject(Map.class);

 completableFutures.add(futureResponse1);
 completableFutures.add(futureResponse2);
 CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).join();
