package com.shipproxy.common;

    import com.fasterxml.jackson.annotation.JsonCreator;
    import com.fasterxml.jackson.annotation.JsonProperty;
    import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
    import com.fasterxml.jackson.databind.annotation.JsonSerialize;
    import com.shipproxy.common.serializers.HttpMethodDeserializer;
    import com.shipproxy.common.serializers.HttpMethodSerializer;
    import org.springframework.http.HttpHeaders;
    import org.springframework.http.HttpMethod;

    public class ProxyRequest {
        private String url;
        private String body;
        private HttpHeaders headers;

        @JsonSerialize(using = HttpMethodSerializer.class)
        @JsonDeserialize(using = HttpMethodDeserializer.class)
        private HttpMethod httpMethod;

        private String requestId;

        @JsonCreator
        public ProxyRequest(@JsonProperty("url") String url,
                            @JsonProperty("body") String body,
                            @JsonProperty("headers") HttpHeaders headers,
                            @JsonProperty("httpMethod") HttpMethod httpMethod,
                            @JsonProperty("requestId") String requestId) {
            this.url = url;
            this.body = body;
            this.headers = headers;
            this.httpMethod = httpMethod;
            this.requestId = requestId;
        }

        public String getUrl() {
            return url;
        }

        public String getBody() {
            return body;
        }

        public HttpHeaders getHeaders() {
            return headers;
        }

        public HttpMethod getHttpMethod() {
            return httpMethod;
        }

        public String getRequestId() {
            return requestId;
        }
    }