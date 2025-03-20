package com.shipproxy.server;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.UnknownHostException;

@RestController
@SpringBootApplication
public class ServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);

    @Autowired
    private WebClient webClient;

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }

    @RequestMapping("/**")
    public Mono<ResponseEntity<String>> handleRequests(HttpServletRequest httpServletRequest, @RequestHeader HttpHeaders headers, @RequestBody(required = false) String body) {

        String host = headers.getFirst("host");
        String url = "http://" + host + httpServletRequest.getRequestURI();
        HttpMethod httpMethod = HttpMethod.valueOf(httpServletRequest.getMethod());
        String requestId = headers.getFirst("X-Request-Id");
        logger.info("Serving request: {} started", requestId);


        logger.debug("Forwarding request to: {}", url);

        return webClient.method(httpMethod).uri(URI.create(url)).headers(httpHeaders -> httpHeaders.addAll(headers)).body(body != null ? BodyInserters.fromValue(body) : BodyInserters.empty()).retrieve().bodyToMono(String.class).map(response -> {
            logger.info("Serving request: {} finished", requestId);
            return ResponseEntity.ok().body(response);
        }).onErrorResume(e -> {
            logger.info("Serving request: {} ran into an error", requestId);
            if (e instanceof UnknownHostException || e.getCause() instanceof UnknownHostException) {
                logger.error("DNS resolution failed: {}", host);
                return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY).header("Content-Type", "text/plain").body("HTTP/1.1 502 Bad Gateway\nError: Unable to resolve domain " + host));
            } else {
                logger.error("Unexpected error: {}", e.getMessage());
                return Mono.just(ResponseEntity.internalServerError().header("Content-Type", "text/plain").body("Internal Server Error: " + e.getMessage()));
            }
        });
    }
}
