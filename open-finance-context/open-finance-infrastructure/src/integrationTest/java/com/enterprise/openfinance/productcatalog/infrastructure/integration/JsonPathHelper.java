package com.enterprise.openfinance.productcatalog.infrastructure.integration;

import com.jayway.jsonpath.JsonPath;

final class JsonPathHelper {

    private JsonPathHelper() {
    }

    static String read(String json, String expression) {
        return JsonPath.read(json, expression);
    }
}
