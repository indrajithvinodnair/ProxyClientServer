package com.shipproxy.client.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.channel.MicrometerChannelMetricsRecorder;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Value("${offshore.proxy.url}")
    private String offshoreProxyUrl;

    @Value("${enable.wiretap}")
    private boolean enableWireTap;

    @Bean
    public WebClient webClient() {
        // Configure connection pool for a SINGLE persistent connection
        ConnectionProvider connectionProvider = ConnectionProvider.builder("fixed")
                .maxConnections(1) // Single connection
                .pendingAcquireTimeout(Duration.ZERO) // Wait indefinitely for a free connection
                .maxIdleTime(Duration.ofSeconds(-1)) // No idle timeout
                .maxLifeTime(Duration.ofSeconds(-1)) // No max lifetime
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(60, TimeUnit.SECONDS))
                )
                .keepAlive(true)    // Enable TCP keep-alive
                .wiretap(enableWireTap);     // Debugging (optional)

        return WebClient.builder()
                .baseUrl(offshoreProxyUrl) // Target offshore server
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
