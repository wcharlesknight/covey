package com.covey.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.covey.config.FirebaseConfig;
import com.covey.middleware.AuthMiddleware;
import com.covey.util.ApiGatewayUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Optional;

public class AuthHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
  private AuthMiddleware authMiddleware;

  public AuthHandler() throws Exception {
    FirebaseAuth firebaseAuth = FirebaseConfig.getAuth();
    this.authMiddleware = new AuthMiddleware(firebaseAuth);
  }

  @Override
  public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
    context.getLogger().log("Auth request received");

    try {
      String authHeader = ApiGatewayUtil.getAuthorizationHeader(event);
      Optional<String> uid = authMiddleware.validateToken(authHeader);

      if (uid.isPresent()) {
        context.getLogger().log("Token validated for user: " + uid.get());
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("statusCode", 200);
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("principalId", uid.get());
        body.put("status", "valid");
        response.put("body", new com.google.gson.Gson().toJson(body));
        return response;
      } else {
        context.getLogger().log("Token validation failed");
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("statusCode", 401);
        response.put("body", "{\"error\": \"Unauthorized\"}");
        return response;
      }
    } catch (Exception e) {
      context.getLogger().log("Error: " + e.getMessage());
      Map<String, Object> response = new java.util.HashMap<>();
      response.put("statusCode", 500);
      response.put("body", "{\"error\": \"Internal server error\"}");
      return response;
    }
  }
}
