package com.networknt.client.builder;

public class ServiceDef {
    private String protocol;
    private String serviceId;
    private String environment;
    private String requestKey;

    // To pick up the environment from config
    public ServiceDef(String protocol, String serviceId, String requestKey) {
        this.protocol = protocol;
        this.serviceId = serviceId;
        this.requestKey = requestKey;
    }

    public ServiceDef(String protocol, String serviceId, String environment, String requestKey) {
        this.protocol = protocol;
        this.serviceId = serviceId;
        this.requestKey = requestKey;
        this.environment = environment;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getRequestKey() {
        return requestKey;
    }

    public void setRequestKey(String requestKey) {
        this.requestKey = requestKey;
    }
}
