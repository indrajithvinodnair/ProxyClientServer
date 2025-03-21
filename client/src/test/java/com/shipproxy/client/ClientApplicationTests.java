package com.shipproxy.client;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
class ClientApplicationTest {

    @Autowired
    private ClientApplication clientApplication;

    @Test
    void testHandleValidRequest() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", "/test");
        mockRequest.addHeader("Test-Header", "value");

        ResponseEntity<String> response = clientApplication.handleRequests(mockRequest, null);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("test", response.getBody());
    }

    @Test
    void testHandlePostRequest() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest("POST", "/test");
        mockRequest.addHeader("Test-Header", "value");

        ResponseEntity<String> response = clientApplication.handleRequests(mockRequest, null);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("test", response.getBody());
    }

    @Test
    void testHandleRequestWithoutHeaders() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", "/test");

        ResponseEntity<String> response = clientApplication.handleRequests(mockRequest, null);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("test", response.getBody());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public WebClient mockWebClient() {
            // Mock the entire WebClient fluent API chain
            WebClient.RequestBodyUriSpec requestBodyUriSpec = Mockito.mock(WebClient.RequestBodyUriSpec.class);
            WebClient.RequestHeadersSpec<?> requestHeadersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
            WebClient.ResponseSpec responseSpec = Mockito.mock(WebClient.ResponseSpec.class);

            WebClient webClient = Mockito.mock(WebClient.class);

            // Set up method chain with explicit casting
            Mockito.when(webClient.method(any(HttpMethod.class))).thenReturn(requestBodyUriSpec);
            Mockito.when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodyUriSpec);
            Mockito.when(requestBodyUriSpec.headers(any())).thenReturn(requestBodyUriSpec);

            // Fix: Add unchecked cast to resolve generic type mismatch
            Mockito.when(requestBodyUriSpec.body(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);

            Mockito.when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            Mockito.when(responseSpec.toEntity(String.class)).thenReturn(Mono.just(ResponseEntity.ok("test")));

            return webClient;
        }
    }
}