package com.shipproxy.client.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
public class WebClientConfig {

    @Value("${offshore.proxy.url}")
    private String offshoreProxyUrl;

    @Value("${enable.wiretap}")
    private boolean enableWireTap;

    @Bean
    public WebClient webClient() {
        // Configure connection pool to create a new connection for each request
        ConnectionProvider connectionProvider = ConnectionProvider.newConnection();

        HttpClient httpClient = HttpClient.create(connectionProvider);

        return WebClient.builder().baseUrl(offshoreProxyUrl) // Target offshore server
                .clientConnector(new ReactorClientHttpConnector(httpClient)).build();
    }
}
