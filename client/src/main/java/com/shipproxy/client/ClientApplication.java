package com.shipproxy.client;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.shipproxy.client.utils.HeaderUtils.getHeaders;

@SpringBootApplication
@RestController
public class ClientApplication {
    private static final Logger logger = LoggerFactory.getLogger(ClientApplication.class);
    private final BlockingQueue<ProxyRequest> requestQueue = new LinkedBlockingQueue<>();
    @Autowired
    private WebClient webClient;

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    @RequestMapping(value = "/**")
    public ResponseEntity<Mono<String>> handleRequests(HttpServletRequest httpServletRequest, @RequestBody(required = false) String body) {
        // validate the request
        String url = httpServletRequest.getRequestURI();
        if (url == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Mono.just("Missing URL parameter"));
        }

        // Extract headers, URI, and HTTP method
        ProxyRequest proxyRequest = new ProxyRequest(url, body, getHeaders(httpServletRequest), HttpMethod.valueOf(httpServletRequest.getMethod()));

        // Add the request to the queue
        requestQueue.add(proxyRequest);

        return ResponseEntity.ok(Mono.just("Request added to client side Queue."));
    }

    @PostConstruct
    public void startProcessing() {
//        TODO : Revisit this part as it introduces a fixed delay of 500ms
        Flux.interval(java.time.Duration.ofMillis(500))
                .flatMap(i -> {
                    ProxyRequest request = requestQueue.poll();
                    return request != null ? forwardRequestReactive(request) : Mono.empty();
                })
                .doOnError(error -> logger.error("Error in processing: {}", error.getMessage()))
                .subscribe(); // Start processing the stream
    }

    private Mono<Void> forwardRequestReactive(ProxyRequest request) {
        return webClient.method(request.getHttpMethod())
                .uri(URI.create(request.getUrl()))
                .headers(httpHeaders -> httpHeaders.addAll(request.getHeaders()))
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(response -> logger.info("Response received: {}", response))
                .then(); // Complete the reactive Mono stream
    }
}