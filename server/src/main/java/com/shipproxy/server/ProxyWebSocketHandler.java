package com.shipproxy.server;

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

    public ProxyWebSocketHandler(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        executorService.submit(() -> {
            try {
                String requestUrl = message.getPayload();
                logger.info("Received request through channel id : {}", session.toString());

                // Forward the request to the actual destination
                ResponseEntity<String> response = webClient.get()
                        .uri(requestUrl)
                        .retrieve()
                        .toEntity(String.class)
                        .block();

                // Send the response back to the client
                session.sendMessage(new TextMessage(response.getBody()));
                logger.info("Response sent through channel id : {}",  session.toString());
                logger.debug("🛠️ Sending response over WebSocket: {}", response);

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
