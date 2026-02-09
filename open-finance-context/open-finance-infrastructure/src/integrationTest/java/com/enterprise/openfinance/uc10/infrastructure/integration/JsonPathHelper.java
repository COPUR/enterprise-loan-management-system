package com.enterprise.openfinance.uc10.infrastructure.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class JsonPathHelper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonPathHelper() {
    }

    static String read(String json, String path) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            String[] tokens = path.replace("$.", "").split("\\.");
            for (String token : tokens) {
                if (token.matches("\\d+")) {
                    node = node.path(Integer.parseInt(token));
                } else {
                    node = node.path(token);
                }
            }
            if (node.isMissingNode() || node.isNull()) {
                return null;
            }
            return node.asText();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to parse path " + path, ex);
        }
    }
}
