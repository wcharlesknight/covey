package com.covey.util;

import java.util.Map;

public class ApiGatewayUtil {
  public static String getAuthorizationHeader(Map<String, Object> event) {
    // Try headers map (case-insensitive lookup)
    @SuppressWarnings("unchecked")
    Map<String, Object> headers = (Map<String, Object>) event.get("headers");
    if (headers != null) {
      for (String key : headers.keySet()) {
        if (key.equalsIgnoreCase("Authorization")) {
          Object value = headers.get(key);
          return value != null ? value.toString() : "";
        }
      }
    }

    // Fallback: try multiValueHeaders
    @SuppressWarnings("unchecked")
    Map<String, Object> multiValueHeaders = (Map<String, Object>) event.get("multiValueHeaders");
    if (multiValueHeaders != null) {
      for (String key : multiValueHeaders.keySet()) {
        if (key.equalsIgnoreCase("Authorization")) {
          Object value = multiValueHeaders.get(key);
          if (value instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<String> list = (java.util.List<String>) value;
            return !list.isEmpty() ? list.get(0) : "";
          }
          return value != null ? value.toString() : "";
        }
      }
    }

    return "";
  }
}
