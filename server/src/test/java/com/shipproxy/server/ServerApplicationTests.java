package com.shipproxy.server;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.UnknownHostException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class ServerApplicationTest {

    @Autowired
    private ServerApplication serverApplication;

    @Autowired
    private WebClient webClient;

    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private HttpServletRequest httpServletRequest;

    @BeforeEach
    void setUp() {
        // Initialize mocks
        requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        responseSpec = mock(WebClient.ResponseSpec.class);

        // Configure WebClient mock
        when(webClient.method(any(HttpMethod.class))).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(URI.class))).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.headers(any())).thenReturn(requestBodyUriSpec);

        // Fix: Use type-safe argument matcher and cast
        when(requestBodyUriSpec.body(any(BodyInserter.class))) // Use specific BodyInserter matcher
                .thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec); // Unchecked cast

        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void testHandleRequests_Success() {
        // Mock request data
        when(httpServletRequest.getRequestURI()).thenReturn("/test");
        when(httpServletRequest.getMethod()).thenReturn("GET");

        HttpHeaders headers = new HttpHeaders();
        headers.add("host", "example.com");
        headers.add("X-Request-Id", "12345");

        // Mock WebClient response
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("Success"));

        Mono<ResponseEntity<String>> response = serverApplication.handleRequests(httpServletRequest, headers, null);

        // Verify response
        ResponseEntity<String> responseEntity = response.block();
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals("Success", responseEntity.getBody());
    }

    @Test
    void testHandleRequests_DnsResolutionFailure() {
        // Mock request data
        when(httpServletRequest.getRequestURI()).thenReturn("/test");
        when(httpServletRequest.getMethod()).thenReturn("GET");

        HttpHeaders headers = new HttpHeaders();
        headers.add("host", "unknownhost.com");
        headers.add("X-Request-Id", "12345");

        // Mock WebClient to throw UnknownHostException
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(new UnknownHostException("DNS resolution failed")));

        Mono<ResponseEntity<String>> response = serverApplication.handleRequests(httpServletRequest, headers, null);

        // Verify response
        ResponseEntity<String> responseEntity = response.block();
        assertEquals(HttpStatus.BAD_GATEWAY, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("502 Bad Gateway"));
    }

    @Test
    void testHandleRequests_UnexpectedError() {
        // Mock request data
        when(httpServletRequest.getRequestURI()).thenReturn("/test");
        when(httpServletRequest.getMethod()).thenReturn("GET");

        HttpHeaders headers = new HttpHeaders();
        headers.add("host", "example.com");
        headers.add("X-Request-Id", "12345");

        // Mock WebClient to throw an unexpected error
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(new RuntimeException("Unexpected error")));

        Mono<ResponseEntity<String>> response = serverApplication.handleRequests(httpServletRequest, headers, null);

        // Verify response
        ResponseEntity<String> responseEntity = response.block();
        assert responseEntity != null;
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertTrue(Objects.requireNonNull(responseEntity.getBody()).contains("Internal Server Error"));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public WebClient mockWebClient() {
            return mock(WebClient.class);
        }
    }
}