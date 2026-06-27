package com.covey.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.covey.config.FirebaseConfig;
import com.covey.middleware.AuthMiddleware;
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

    String authHeader = (String) event.getOrDefault("authorizationToken", "");
    Optional<String> uid = authMiddleware.validateToken(authHeader);

    if (uid.isPresent()) {
      context.getLogger().log("Token validated for user: " + uid.get());
      JsonObject response = new JsonObject();
      response.addProperty("principalId", uid.get());
      response.addProperty("status", "valid");
      return response.asMap();
    } else {
      context.getLogger().log("Token validation failed");
      throw new RuntimeException("Unauthorized");
    }
  }
}
