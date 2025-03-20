package com.shipproxy.client.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

import java.util.Enumeration;


public class HeaderUtils {
    public static HttpHeaders getHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();

        // Retrieve header names from the request
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                Enumeration<String> values = request.getHeaders(headerName);

                // Add all values for each header
                while (values.hasMoreElements()) {
                    String value = values.nextElement();
                    headers.add(headerName, value);
                }
            }
        }

        return headers;
    }
}
