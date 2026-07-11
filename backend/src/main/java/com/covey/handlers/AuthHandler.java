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

    String authHeader = ApiGatewayUtil.getAuthorizationHeader(event);
    Optional<String> uid = authMiddleware.validateToken(authHeader);

    if (uid.isPresent()) {
      context.getLogger().log("Token validated for user: " + uid.get());
      Map<String, Object> response = new java.util.HashMap<>();
      response.put("principalId", uid.get());
      response.put("status", "valid");
      return response;
    } else {
      context.getLogger().log("Token validation failed");
      throw new RuntimeException("Unauthorized");
    }
  }
}
