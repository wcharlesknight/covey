package com.covey.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.covey.config.FirebaseConfig;
import com.covey.middleware.AuthMiddleware;
import com.covey.models.Invite;
import com.covey.util.ApiGatewayUtil;
import com.covey.models.Invite.Status;
import com.covey.services.RsvpService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RsvpHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
  private final AuthMiddleware authMiddleware;
  private final RsvpService rsvpService;
  private final Gson gson;

  public RsvpHandler() throws Exception {
    FirebaseAuth firebaseAuth = FirebaseConfig.getAuth();
    this.authMiddleware = new AuthMiddleware(firebaseAuth);
    this.rsvpService = new RsvpService();
    this.gson = new Gson();
  }

  @Override
  public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
    context.getLogger().log("POST /invites/{id}/rsvp request");

    try {
      String authHeader = ApiGatewayUtil.getAuthorizationHeader(event);
      Optional<String> uid = authMiddleware.validateToken(authHeader);

      if (!uid.isPresent()) {
        return error(401, "Unauthorized");
      }

      // Extract invite ID from path or pathParameters
      String inviteId = "";

      // First try pathParameters (normal API Gateway)
      Object pathParamsObj = event.getOrDefault("pathParameters", null);
      if (pathParamsObj != null) {
        Map<String, String> params = gson.fromJson(gson.toJson(pathParamsObj), Map.class);
        inviteId = params.getOrDefault("id", "");
      }

      // If not found, extract from path (for direct invocation)
      if (inviteId.isEmpty()) {
        String path = (String) event.getOrDefault("path", "");
        if (path.startsWith("/dev/")) {
          path = path.substring(4);
        }
        if (path.startsWith("/prod/")) {
          path = path.substring(5);
        }

        // Extract ID from /invites/{id}/rsvp
        String[] parts = path.split("/");
        if (parts.length >= 3 && "invites".equals(parts[1])) {
          inviteId = parts[2];
        }
      }

      if (inviteId.isEmpty()) {
        return error(400, "Invite ID required");
      }

      String bodyStr = (String) event.getOrDefault("body", "{}");
      JsonObject updateData = gson.fromJson(bodyStr, JsonObject.class);

      if (!updateData.has("status")) {
        return error(400, "Status field required");
      }

      String statusStr = updateData.get("status").getAsString();
      Status newStatus;
      try {
        newStatus = Status.valueOf(statusStr.toUpperCase());
      } catch (IllegalArgumentException e) {
        return error(400, "Invalid status: " + statusStr);
      }

      Optional<Invite> updatedInvite = rsvpService.rsvp(inviteId, uid.get(), newStatus);

      if (!updatedInvite.isPresent()) {
        return error(404, "Invite not found");
      }

      Map<String, Object> response = new HashMap<>();
      response.put("statusCode", 200);
      response.put("body", gson.toJson(updatedInvite.get()));
      return response;
    } catch (IllegalAccessException e) {
      return error(403, "Forbidden: " + e.getMessage());
    } catch (IllegalArgumentException e) {
      return error(400, e.getMessage());
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
