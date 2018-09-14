package com.networknt.client.model;

import java.util.Map;

public class ConsumerConfig {
    private Map<String, String> serviceEnv;

    public Map<String, String> getServiceEnv() {
        return serviceEnv;
    }

    public void setServiceEnv(Map<String, String> serviceEnv) {
        this.serviceEnv = serviceEnv;
    }
}
