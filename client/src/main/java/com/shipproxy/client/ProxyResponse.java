package com.shipproxy.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;

@Data
@AllArgsConstructor
public class ProxyResponse {
    private String requestId;
    private ResponseEntity<String> response;
}