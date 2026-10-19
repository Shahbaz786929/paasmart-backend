package com.paasmart.backend.common;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public final class RestClients {
    private RestClients() {}

    // Bina timeout ke RestTemplate bahar ki service atak jaye to request hamesha latki rehti hai
    public static RestTemplate withTimeouts(int connectMs, int readMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectMs);
        factory.setReadTimeout(readMs);
        return new RestTemplate(factory);
    }
}