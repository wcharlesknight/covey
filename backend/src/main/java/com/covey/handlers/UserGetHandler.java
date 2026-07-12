package com.covey.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.covey.config.FirebaseConfig;
import com.covey.middleware.AuthMiddleware;
import com.covey.models.User;
import com.covey.util.ApiGatewayUtil;
import com.covey.services.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
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
      String authHeader = ApiGatewayUtil.getAuthorizationHeader(event);
      Optional<FirebaseToken> token = authMiddleware.decodeToken(authHeader);

      if (!token.isPresent()) {
        return error(401, "Unauthorized");
      }

      FirebaseToken decoded = token.get();
      String uid = decoded.getUid();

      Optional<User> existing = userService.getUser(uid);
      if (existing.isPresent()) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", 200);
        response.put("body", gson.toJson(existing.get()));
        return response;
      }

      // Auto-provision on first sign-in (WBS 1.3.2.3)
      String email = decoded.getEmail() != null ? decoded.getEmail() : "";
      String displayName = decoded.getName() != null ? decoded.getName() : "";
      User newUser = new User(uid, email, displayName, null);
      userService.createUser(newUser);
      context.getLogger().log("Auto-provisioned user: " + uid);

      Map<String, Object> response = new HashMap<>();
      response.put("statusCode", 200);
      response.put("body", gson.toJson(newUser));
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
