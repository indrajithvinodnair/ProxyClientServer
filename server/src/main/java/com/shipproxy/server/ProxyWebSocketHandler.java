package com.shipproxy.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shipproxy.common.ProxyRequest;
import com.shipproxy.common.ProxyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProxyWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(ProxyWebSocketHandler.class);
    private final WebClient webClient;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(); // Ensures sequential processing
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProxyWebSocketHandler(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        executorService.submit(() -> {
            try {
                String payload = message.getPayload();
                logger.debug("Received message: {}", payload);

                // Deserialize the message to ProxyRequest
                ProxyRequest proxyRequest = objectMapper.readValue(payload, ProxyRequest.class);
               logger.info("Recieved request with ID: {} through WebSocket channelId: {}", proxyRequest.getRequestId(), session.toString());
                logger.debug("📤 Sending to WebClient: {}", objectMapper.writeValueAsString(proxyRequest));


                // Ensure body is not null
                String body = proxyRequest.getBody() != null ? proxyRequest.getBody() : "";

                WebClient.RequestBodySpec requestSpec = webClient.method(proxyRequest.getHttpMethod()).uri(proxyRequest.getUrl()).headers(headers -> headers.addAll(proxyRequest.getHeaders()));

                WebClient.ResponseSpec responseSpec;
                if (proxyRequest.getBody() != null && !proxyRequest.getBody().isEmpty()) {
                    responseSpec = requestSpec.bodyValue(proxyRequest.getBody()).retrieve();
                } else {
                    responseSpec = requestSpec.retrieve();
                }

                // Process the response
                ResponseEntity<String> responseEntity = responseSpec.toEntity(String.class).block();

                // Create ProxyResponse
                ProxyResponse.Response response = new ProxyResponse.Response(responseEntity.getBody());
                ProxyResponse proxyResponse = new ProxyResponse(proxyRequest.getRequestId(), response);

                // Serialize ProxyResponse to JSON
                String responsePayload = objectMapper.writeValueAsString(proxyResponse);


                // Send the response back to the client
                session.sendMessage(new TextMessage(responsePayload));
                logger.debug("Response sent: {}", responsePayload);
                logger.info("Response sent for request ID: {} through WebSocket channelId: {}", proxyRequest.getRequestId(), session.toString());
            } catch (Exception e) {
                logger.error("Error processing request: {}", e.getMessage());
            }
        });
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = session.getId(); // WebSocket Session ID
        String channelId = session.toString(); // Includes Netty Channel details

        logger.info("✅ WebSocket connection established with client.");
        logger.info("🔗 WebSocket Session ID: {}", sessionId);
        logger.info("🛠️ Netty TCP Channel Info: {}", channelId);
    }
}