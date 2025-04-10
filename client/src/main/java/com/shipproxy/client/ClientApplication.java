package com.shipproxy.client;

    import com.fasterxml.jackson.core.JsonProcessingException;
    import com.fasterxml.jackson.databind.JsonMappingException;
    import com.fasterxml.jackson.databind.ObjectMapper;
    import com.shipproxy.common.ProxyRequest;
    import com.shipproxy.common.ProxyResponse;
    import jakarta.annotation.PostConstruct;
    import jakarta.annotation.PreDestroy;
    import jakarta.servlet.http.HttpServletRequest;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.boot.SpringApplication;
    import org.springframework.boot.autoconfigure.SpringBootApplication;
    import org.springframework.http.HttpMethod;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.RequestBody;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RestController;
    import org.springframework.web.socket.*;
    import org.springframework.web.socket.client.standard.StandardWebSocketClient;
    import org.springframework.web.socket.handler.TextWebSocketHandler;

    import java.net.URI;
    import java.util.UUID;
    import java.util.concurrent.*;

    import static com.shipproxy.client.utils.HeaderUtils.getHeaders;

    @SpringBootApplication
    @RestController
    public class ClientApplication {
        private static final Logger logger = LoggerFactory.getLogger(ClientApplication.class);
        private final BlockingQueue<ProxyRequest> requestQueue = new LinkedBlockingQueue<>();
        private final BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();
        private volatile WebSocketSession session;
        private final ObjectMapper objectMapper = new ObjectMapper();
        private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();


        @Value("${offshore.proxy.url}")
        private String offshoreProxyUrl;

        public static void main(String[] args) {
            SpringApplication.run(ClientApplication.class, args);
        }

        @PostConstruct
        public void init() {
            connectWebSocket();
            startProcessing();
        }

       private void connectWebSocket() {
    try {
        logger.info("Attempting initial WebSocket connection...");

        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketHandler handler = new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                responseQueue.add(message.getPayload());  // Add response to queue
            }

            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
                ClientApplication.this.session = session;
                String sessionId = session.getId(); // WebSocket session ID
                String channelId = session.toString(); // Includes Netty TCP Channel info

                logger.info("✅ WebSocket connection established with server {}.", offshoreProxyUrl);
                logger.info("🔗 WebSocket Session ID: {}", sessionId);
                logger.info("🛠️ Netty TCP Channel Info: {}", channelId);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                logger.warn("❌ WebSocket connection closed. Status: {}", status);
                ClientApplication.this.session = null;
                scheduleReconnection(); // Schedule reconnection when the connection is lost
            }
        };

        String wsUrl = offshoreProxyUrl.replace("http://", "ws://");
        session = client.doHandshake(handler, new WebSocketHttpHeaders(), URI.create(wsUrl + "/ws")).get();
        logger.info("✅ WebSocket connected successfully to {}.", offshoreProxyUrl);

    } catch (Exception e) {
        logger.error("❌ Initial WebSocket connection failed. Retrying...", e);
        scheduleReconnection(); // Schedule reconnection if the initial connection fails
    }
}

        private void scheduleReconnection() {
            scheduledExecutor.scheduleWithFixedDelay(() -> {
                if (session == null || !session.isOpen()) {
                    try {
                        logger.info("Attempting WebSocket reconnection...");
                        connectWebSocket();
                    } catch (Exception e) {
                        logger.error("❌ WebSocket reconnection failed. Retrying...", e);
                    }
                }
            }, 0, 5, TimeUnit.SECONDS);
        }

        private void startProcessing() {
            singleThreadExecutor.submit(() -> {
                while (true) {
                    try {
                        ProxyRequest request = requestQueue.take();
                        logger.info("🚀 Sending request {} over WebSocket (Channel ID: {}): {}", request.getRequestId(), session.getId(), request.getUrl());

                        if (session == null || !session.isOpen()) {
                            logger.warn("WebSocket session is closed, waiting...");
                            continue;
                        }

                        String jsonRequest = objectMapper.writeValueAsString(request);
                        session.sendMessage(new TextMessage(jsonRequest));

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.error("Error processing request:", e);
                    } catch (JsonProcessingException e) {
                        logger.error("Error serializing request to JSON:", e);
                    }
                }
            });
        }
        @RequestMapping(value = "/**")
        public ResponseEntity<String> handleRequests(HttpServletRequest request, @RequestBody(required = false) String body) {
            String requestId = UUID.randomUUID().toString();
            String fullUrl = request.getRequestURL().toString();
            if (request.getQueryString() != null) {
                fullUrl += "?" + request.getQueryString();
            }
            ProxyRequest proxyRequest = new ProxyRequest(fullUrl, body, getHeaders(request), HttpMethod.valueOf(request.getMethod()), requestId);

            try {
                requestQueue.put(proxyRequest);

                // Wait for response
                String responseBody = responseQueue.take();
                ProxyResponse proxyResponse = objectMapper.readValue(responseBody, ProxyResponse.class);
                logger.info("🚀 Received response for request {}: through WebSocket (Channel ID: {})", proxyResponse.getRequestId(), session.getId());
                logger.debug("Response: {}", proxyResponse.getResponse().getBody());
                return ResponseEntity.ok(proxyResponse.getResponse().getBody());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
            } catch (JsonMappingException e) {
                throw new RuntimeException(e);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

      @PreDestroy
         public void shutdown() {
             try {
                 logger.info("Shutting down executors...");
                 scheduledExecutor.shutdown();
                 singleThreadExecutor.shutdown();
                 if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                     scheduledExecutor.shutdownNow();
                 }
                 if (!singleThreadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                     singleThreadExecutor.shutdownNow();
                 }
             } catch (InterruptedException e) {
                 Thread.currentThread().interrupt();
                 logger.error("Error during shutdown:", e);
             }
         }
    }