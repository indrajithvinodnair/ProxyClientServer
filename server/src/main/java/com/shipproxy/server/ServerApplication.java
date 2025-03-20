package com.shipproxy.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;

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
	public Mono<ResponseEntity<String>> handleRequests(
			HttpServletRequest httpServletRequest,
			@RequestHeader HttpHeaders headers,
			@RequestBody(required = false) String body) {

		logger.debug("Incoming request: URI = {}, Method = {}", httpServletRequest.getRequestURI(), httpServletRequest.getMethod());
		logger.debug("Request Headers: {}", headers);
		logger.debug("Request Body: {}", body);

		String host = headers.getFirst("host");
		String url = "http://" + host + httpServletRequest.getRequestURI();
		logger.debug("Constructed URL: {}", url);

		HttpMethod httpMethod = HttpMethod.valueOf(httpServletRequest.getMethod());
		logger.debug("HTTP Method: {}", httpMethod);

		logger.debug("Forwarding request: Method = {}, URL = {}, Headers = {}, Body = {}",
				httpMethod, url, headers, body);


		return webClient.method(httpMethod)
				.uri(URI.create(url))
				.headers(httpHeaders -> httpHeaders.addAll(headers))
				.body(body != null ? BodyInserters.fromValue(body) : BodyInserters.empty())
				.retrieve()
				.toEntity(String.class) // ✅ Return full response including headers & status
				.map(responseEntity -> ResponseEntity
						.status(responseEntity.getStatusCode())
						.headers(responseEntity.getHeaders())
						.body(responseEntity.getBody()))
				.onErrorResume(e -> {
					logger.error("Error forwarding request:", e);
					return Mono.just(ResponseEntity.internalServerError().body("Error: " + e.getMessage()));
				});

	}




}
