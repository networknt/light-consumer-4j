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
    <version>1.5.26</version>
</dependency>
```
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
 
 
 #### Here is the code example to call the api.
 
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
