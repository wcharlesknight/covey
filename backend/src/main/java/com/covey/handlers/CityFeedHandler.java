package com.covey.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.covey.config.FirebaseConfig;
import com.covey.middleware.AuthMiddleware;
import com.covey.models.Rsvp;
import com.covey.models.User;
import com.covey.models.WeeklySpot;
import com.covey.services.FeedService;
import com.covey.services.SpotRsvpService;
import com.covey.services.UserService;
import com.covey.util.ApiGatewayUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CityFeedHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
  private static final int FEED_LIMIT = 4;

  private final AuthMiddleware authMiddleware;
  private final UserService userService;
  private final FeedService feedService;
  private final SpotRsvpService spotRsvpService;
  private final Gson gson;

  public CityFeedHandler() throws Exception {
    FirebaseAuth firebaseAuth = FirebaseConfig.getAuth();
    this.authMiddleware = new AuthMiddleware(firebaseAuth);
    this.userService = new UserService();
    this.feedService = new FeedService();
    this.spotRsvpService = new SpotRsvpService();
    this.gson = new Gson();
  }

  @Override
  public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
    context.getLogger().log("GET /feed request");

    try {
      String authHeader = ApiGatewayUtil.getAuthorizationHeader(event);
      Optional<String> uid = authMiddleware.validateToken(authHeader);

      if (!uid.isPresent()) {
        return error(401, "Unauthorized");
      }

      // Viewing city comes from the query string; fall back to the user's chosen city.
      String city = ApiGatewayUtil.getQueryParam(event, "city");
      if (city == null || city.isEmpty()) {
        Optional<User> userOptional = userService.getUser(uid.get());
        if (!userOptional.isPresent()) {
          return error(404, "User not found");
        }
        city = userOptional.get().getCity();
      }
      if (city == null || city.isEmpty()) {
        return error(400, "No city specified and user has no chosen city");
      }

      List<WeeklySpot> spots = feedService.getRecentSpotsForCity(city, FEED_LIMIT);

      List<Map<String, Object>> feedItems = new ArrayList<>();
      for (WeeklySpot spot : spots) {
        List<Rsvp> rsvps = spotRsvpService.getRsvpsForSpot(spot.getId());

        Map<String, Integer> counts = new HashMap<>();
        counts.put("yes", 0);
        counts.put("no", 0);
        counts.put("interested", 0);
        String userRsvp = null;
        for (Rsvp rsvp : rsvps) {
          if (rsvp.getStatus() == null) {
            continue;
          }
          switch (rsvp.getStatus()) {
            case YES:
              counts.put("yes", counts.get("yes") + 1);
              break;
            case NO:
              counts.put("no", counts.get("no") + 1);
              break;
            case INTERESTED:
              counts.put("interested", counts.get("interested") + 1);
              break;
            default:
              break;
          }
          if (uid.get().equals(rsvp.getUserId())) {
            userRsvp = rsvp.getStatus().name();
          }
        }

        Map<String, Object> item = new HashMap<>();
        item.put("spot", spot);
        item.put("counts", counts);
        item.put("userRsvp", userRsvp);
        feedItems.add(item);
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
