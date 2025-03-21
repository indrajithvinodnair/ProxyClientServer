package com.shipproxy.client.utils;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeaderUtilsTest {

    @Test
    void testGetHeaders() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader("Content-Type", "application/json");
        mockRequest.addHeader("Authorization", "Bearer token");

        HttpHeaders headers = HeaderUtils.getHeaders(mockRequest);

        assertEquals("application/json", headers.getFirst("Content-Type"));
        assertEquals("Bearer token", headers.getFirst("Authorization"));
    }
}