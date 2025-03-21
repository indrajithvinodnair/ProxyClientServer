package com.shipproxy.client;

import org.springframework.http.ResponseEntity;

public class ProxyResponse {
    private String requestId;
    private ResponseEntity<String> response;

    public ProxyResponse(String requestId, ResponseEntity<String> response) {
        this.requestId = requestId;
        this.response = response;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public ResponseEntity<String> getResponse() {
        return response;
    }

    public void setResponse(ResponseEntity<String> response) {
        this.response = response;
    }
}