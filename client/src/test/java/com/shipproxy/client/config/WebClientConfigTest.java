package com.shipproxy.client.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class WebClientConfigTest {

    @Test
    public void testWebClientBean() {
        WebClientConfig webClientConfig = new WebClientConfig();
        WebClient webClient = webClientConfig.webClient();
        assertNotNull(webClient);
        assertNotNull(webClientConfig);
    }
}