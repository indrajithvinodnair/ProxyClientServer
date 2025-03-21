package com.shipproxy.client;


import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

public class ProxyRequest {
    private String url;
    private String body;
    private HttpHeaders headers;
    private HttpMethod httpMethod;
    private String requestId;

    public ProxyRequest(String url, String body, HttpHeaders headers, HttpMethod httpMethod, String requestId) {
        this.url = url;
        this.body = body;
        this.headers = headers;
        this.httpMethod = httpMethod;
        this.requestId = requestId;
    }

    public String getUrl() {
        return url;
    }

    public String getBody() {
        return body;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public String getRequestId() {
        return requestId;
    }
    

}
