package com.covey.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.covey.config.FirebaseConfig;
import com.covey.middleware.AuthMiddleware;
import com.covey.models.Rsvp;
import com.covey.models.WeeklySpot;
import com.covey.services.FeedService;
import com.covey.services.SpotRsvpService;
import com.covey.util.ApiGatewayUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SpotRsvpHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
  private final AuthMiddleware authMiddleware;
  private final FeedService feedService;
  private final SpotRsvpService spotRsvpService;
  private final Gson gson;

  public SpotRsvpHandler() throws Exception {
    FirebaseAuth firebaseAuth = FirebaseConfig.getAuth();
    this.authMiddleware = new AuthMiddleware(firebaseAuth);
    this.feedService = new FeedService();
    this.spotRsvpService = new SpotRsvpService();
    this.gson = new Gson();
  }

  @Override
  public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
    context.getLogger().log("POST /rsvp request");

    try {
      String authHeader = ApiGatewayUtil.getAuthorizationHeader(event);
      Optional<String> uid = authMiddleware.validateToken(authHeader);

      if (!uid.isPresent()) {
        return error(401, "Unauthorized");
      }

      String bodyStr = (String) event.getOrDefault("body", "{}");
      JsonObject body = gson.fromJson(bodyStr, JsonObject.class);

      if (body == null || !body.has("spotId") || !body.has("status")) {
        return error(400, "spotId and status fields required");
      }

      String spotId = body.get("spotId").getAsString();
      String statusStr = body.get("status").getAsString();

      Rsvp.Status status;
      try {
        status = Rsvp.Status.valueOf(statusStr.toUpperCase());
      } catch (IllegalArgumentException e) {
        return error(400, "Invalid status: " + statusStr);
      }

      // Load the spot to validate it exists and to read the authoritative city/weekId.
      WeeklySpot spot = feedService.getWeeklySpot(spotId);
      if (spot == null) {
        return error(404, "Spot not found");
      }

      Rsvp rsvp = spotRsvpService.upsert(spotId, uid.get(), spot.getCity(), spot.getWeekId(), status);

      Map<String, Object> response = new HashMap<>();
      response.put("statusCode", 200);
      response.put("body", gson.toJson(rsvp));
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
