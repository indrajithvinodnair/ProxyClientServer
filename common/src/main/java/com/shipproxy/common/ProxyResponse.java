package com.shipproxy.common;

        import com.fasterxml.jackson.annotation.JsonCreator;
        import com.fasterxml.jackson.annotation.JsonProperty;

        public class ProxyResponse {
            private String requestId;
            private Response response;

            public static class Response {
                private String body;

                @JsonCreator
                public Response(@JsonProperty("body") String body) {
                    this.body = body;
                }

                public String getBody() {
                    return body;
                }

                public void setBody(String body) {
                    this.body = body;
                }
            }

            @JsonCreator
            public ProxyResponse(@JsonProperty("requestId") String requestId,
                                 @JsonProperty("response") Response response) {
                this.requestId = requestId;
                this.response = response;
            }

            public String getRequestId() {
                return requestId;
            }

            public void setRequestId(String requestId) {
                this.requestId = requestId;
            }

            public Response getResponse() {
                return response;
            }

            public void setResponse(Response response) {
                this.response = response;
            }
        }