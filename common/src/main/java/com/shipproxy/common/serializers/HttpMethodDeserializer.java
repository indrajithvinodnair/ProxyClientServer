package com.shipproxy.common.serializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.http.HttpMethod;

import java.io.IOException;

public class HttpMethodDeserializer extends JsonDeserializer<HttpMethod> {
    @Override
    public HttpMethod deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return HttpMethod.valueOf(jsonParser.getText().toUpperCase());
    }
}