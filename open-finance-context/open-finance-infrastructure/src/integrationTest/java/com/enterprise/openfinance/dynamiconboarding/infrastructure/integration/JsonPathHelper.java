package com.enterprise.openfinance.dynamiconboarding.infrastructure.integration;

import com.jayway.jsonpath.JsonPath;

final class JsonPathHelper {

    private JsonPathHelper() {
    }

    static String read(String json, String path) {
        return JsonPath.read(json, path);
    }
}
