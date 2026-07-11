package com.covey.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.covey.config.FirebaseConfig;
import com.covey.middleware.AuthMiddleware;
import com.covey.services.PushTokenService;
import com.covey.util.ApiGatewayUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PushTokenHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
  private final AuthMiddleware authMiddleware;
  private final PushTokenService pushTokenService;
  private final Gson gson;

  public PushTokenHandler() throws Exception {
    FirebaseAuth firebaseAuth = FirebaseConfig.getAuth();
    this.authMiddleware = new AuthMiddleware(firebaseAuth);
    this.pushTokenService = new PushTokenService();
    this.gson = new Gson();
  }

  @Override
  public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
    context.getLogger().log("POST /push-tokens request");

    try {
      String authHeader = ApiGatewayUtil.getAuthorizationHeader(event);
      Optional<String> uid = authMiddleware.validateToken(authHeader);

      if (!uid.isPresent()) {
        return error(401, "Unauthorized");
      }

      String bodyStr = (String) event.getOrDefault("body", "{}");
      JsonObject requestData = gson.fromJson(bodyStr, JsonObject.class);

      if (!requestData.has("token")) {
        return error(400, "Token field required");
      }

      String token = requestData.get("token").getAsString();
      String platform = requestData.has("platform") ? requestData.get("platform").getAsString() : "ios";

      if (token.isEmpty()) {
        return error(400, "Token cannot be empty");
      }

      String tokenId = pushTokenService.registerToken(uid.get(), token, platform);

      Map<String, Object> response = new HashMap<>();
      response.put("statusCode", 201);

      Map<String, String> body = new HashMap<>();
      body.put("id", tokenId);
      body.put("message", "Token registered successfully");
      response.put("body", gson.toJson(body));

      return response;
    } catch (Exception e) {
      context.getLogger().log("Error: " + e.getMessage());
      return error(500, "Internal server error");
    }
  }

  private Map<String, Object> error(int statusCode, String message) {
    Map<String, Object> response = new HashMap<>();
    response.put("statusCode", statusCode);
    response.put("body", "{\"error\": \"" + message + "\"}");
    return response;
  }
}
