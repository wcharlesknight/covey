package com.covey.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.covey.config.FirebaseConfig;
import com.covey.middleware.AuthMiddleware;
import com.covey.models.User;
import com.covey.util.ApiGatewayUtil;
import com.covey.services.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class UserPatchHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
  private final AuthMiddleware authMiddleware;
  private final UserService userService;
  private final Gson gson;

  public UserPatchHandler() throws Exception {
    FirebaseAuth firebaseAuth = FirebaseConfig.getAuth();
    this.authMiddleware = new AuthMiddleware(firebaseAuth);
    this.userService = new UserService();
    this.gson = new Gson();
  }

  @Override
  public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
    context.getLogger().log("PATCH /me request");

    try {
      String authHeader = ApiGatewayUtil.getAuthorizationHeader(event);
      Optional<String> uid = authMiddleware.validateToken(authHeader);

      if (!uid.isPresent()) {
        return error(401, "Unauthorized");
      }

      Optional<User> existingUser = userService.getUser(uid.get());
      if (!existingUser.isPresent()) {
        return error(404, "User not found");
      }

      String bodyStr = (String) event.getOrDefault("body", "{}");
      JsonObject updateData = gson.fromJson(bodyStr, JsonObject.class);

      User user = existingUser.get();

      if (updateData.has("displayName") && updateData.get("displayName").isJsonPrimitive()) {
        user.setDisplayName(updateData.get("displayName").getAsString());
      }
      if (updateData.has("email") && updateData.get("email").isJsonPrimitive()) {
        user.setEmail(updateData.get("email").getAsString());
      }
      if (updateData.has("city") && updateData.get("city").isJsonPrimitive()) {
        user.setCity(updateData.get("city").getAsString());
      }

      userService.saveUser(user);

      Map<String, Object> response = new HashMap<>();
      response.put("statusCode", 200);
      response.put("body", gson.toJson(user));
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
