package com.covey.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.covey.config.FirebaseConfig;
import com.covey.middleware.AuthMiddleware;
import com.covey.models.Invite;
import com.covey.models.User;
import com.covey.models.WeeklySpot;
import com.covey.services.FeedService;
import com.covey.services.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UserFeedHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
  private final AuthMiddleware authMiddleware;
  private final UserService userService;
  private final FeedService feedService;
  private final Gson gson;

  public UserFeedHandler() throws Exception {
    FirebaseAuth firebaseAuth = FirebaseConfig.getAuth();
    this.authMiddleware = new AuthMiddleware(firebaseAuth);
    this.userService = new UserService();
    this.feedService = new FeedService();
    this.gson = new Gson();
  }

  @Override
  public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
    context.getLogger().log("GET /me/feed request");

    try {
      String authHeader = (String) event.getOrDefault("authorizationToken", "");
      Optional<String> uid = authMiddleware.validateToken(authHeader);

      if (!uid.isPresent()) {
        return error(401, "Unauthorized");
      }

      Optional<User> userOptional = userService.getUser(uid.get());
      if (!userOptional.isPresent()) {
        return error(404, "User not found");
      }

      User user = userOptional.get();
      String city = user.getCity();
      if (city == null || city.isEmpty()) {
        return error(400, "User must select a city");
      }

      List<Invite> invites = feedService.getUserFeed(uid.get(), city);

      List<Map<String, Object>> feedItems = new ArrayList<>();
      for (Invite invite : invites) {
        WeeklySpot spot = feedService.getWeeklySpot(invite.getWeeklySpotId());
        if (spot != null) {
          Map<String, Object> item = new HashMap<>();
          item.put("invite", invite);
          item.put("spot", spot);
          feedItems.add(item);
        }
      }

      Map<String, Object> response = new HashMap<>();
      response.put("statusCode", 200);
      response.put("body", gson.toJson(feedItems));
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
