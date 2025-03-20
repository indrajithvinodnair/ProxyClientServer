package com.shipproxy.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

@Data
@AllArgsConstructor
public class ProxyRequest {
    private String url;
    private String body;
    private HttpHeaders headers;
    private HttpMethod httpMethod;
}
