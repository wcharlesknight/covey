package com.covey.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.covey.config.FirebaseConfig;
import com.covey.middleware.AuthMiddleware;
import com.covey.models.User;
import com.covey.services.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class UserGetHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
  private final AuthMiddleware authMiddleware;
  private final UserService userService;
  private final Gson gson;

  public UserGetHandler() throws Exception {
    FirebaseAuth firebaseAuth = FirebaseConfig.getAuth();
    this.authMiddleware = new AuthMiddleware(firebaseAuth);
    this.userService = new UserService();
    this.gson = new Gson();
  }

  @Override
  public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
    context.getLogger().log("GET /me request");

    try {
      String authHeader = (String) event.getOrDefault("authorizationToken", "");
      Optional<String> uid = authMiddleware.validateToken(authHeader);

      if (!uid.isPresent()) {
        return error(401, "Unauthorized");
      }

      Optional<User> user = userService.getUser(uid.get());
      if (!user.isPresent()) {
        return error(404, "User not found");
      }

      Map<String, Object> response = new HashMap<>();
      response.put("statusCode", 200);
      response.put("body", gson.toJson(user.get()));
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
