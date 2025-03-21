package com.shipproxy.client;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static com.shipproxy.client.utils.HeaderUtils.getHeaders;

@SpringBootApplication
@RestController
public class ClientApplication {
    private static final Logger logger = LoggerFactory.getLogger(ClientApplication.class);
    private final BlockingQueue<ProxyRequest> requestQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<ProxyResponse> responseQueue = new LinkedBlockingQueue<>();

    @Value("${offshore.proxy.url}")
    private String offshoreProxyUrl;

    @Autowired
    private WebClient webClient;

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    @RequestMapping(value = "/**")
    public ResponseEntity<String> handleRequests(HttpServletRequest httpServletRequest, @RequestBody(required = false) String body) {
        String url = httpServletRequest.getRequestURI();
        if (url == null) {
            return ResponseEntity.badRequest().body("Missing URL parameter");
        }

        String requestId = UUID.randomUUID().toString();
        ProxyRequest proxyRequest = new ProxyRequest(url, body, getHeaders(httpServletRequest), HttpMethod.valueOf(httpServletRequest.getMethod()), requestId);

        try {
            requestQueue.put(proxyRequest);

            // Wait for response in the response queue
            ProxyResponse response = responseQueue.take();

            return response.getResponse();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.internalServerError().body("Error processing request: Interrupted");
        }
    }


    @PostConstruct
    public void startProcessing() {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                while (true) {
                    ProxyRequest request = requestQueue.take(); // Wait for a request to process
                    logger.info("Processing request: {}", request.getRequestId());

                    ResponseEntity<String> response = forwardRequestReactive(request).block();

                    // Push response to response queue
                    responseQueue.put(new ProxyResponse(request.getRequestId(), response));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Processing interrupted", e);
            } catch (Exception e) {
                logger.error("Error processing request:", e);
            }
        });
    }


    private Mono<ResponseEntity<String>> forwardRequestReactive(ProxyRequest request) {
        return webClient.method(request.getHttpMethod()).uri(offshoreProxyUrl + request.getUrl()) //Ensure Offshore Proxy URL is correct
                .headers(httpHeaders -> {
                    httpHeaders.addAll(request.getHeaders());
                    httpHeaders.add("X-Request-Id", request.getRequestId()); // Add UUID header
                }).body(request.getBody() != null ? BodyInserters.fromValue(request.getBody()) : BodyInserters.empty()).retrieve().toEntity(String.class) // Get full response with headers & status
                .map(responseEntity -> {
                    logger.info("Forwarding response for: {}", request.getRequestId());
                    logger.debug("Forwarding response to client, response body: {}", responseEntity.getBody());

                    //Ensure correct content type is returned
                    HttpHeaders responseHeaders = new HttpHeaders();
                    responseHeaders.addAll(responseEntity.getHeaders());
                    responseHeaders.setContentType(MediaType.TEXT_HTML);

                    return ResponseEntity.status(responseEntity.getStatusCode()).headers(responseHeaders).body(responseEntity.getBody());
                }).onErrorResume(e -> {
                    logger.error("Error forwarding response to client: {}", e.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().body("Error: " + e.getMessage()));
                });
    }
}