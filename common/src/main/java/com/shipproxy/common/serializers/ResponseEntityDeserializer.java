package com.shipproxy.common.serializers;

        import com.fasterxml.jackson.core.JsonParser;
        import com.fasterxml.jackson.core.JsonProcessingException;
        import com.fasterxml.jackson.databind.DeserializationContext;
        import com.fasterxml.jackson.databind.JsonDeserializer;
        import com.fasterxml.jackson.databind.JsonNode;

        import java.io.IOException;

        public class ResponseEntityDeserializer extends JsonDeserializer<String> {
            @Override
            public String deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
                JsonNode node = jsonParser.getCodec().readTree(jsonParser);
                return node.get("body").asText();
            }
        }