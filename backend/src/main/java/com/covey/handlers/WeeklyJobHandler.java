package com.covey.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.covey.config.FirebaseConfig;
import com.covey.integrations.GooglePlacesClient;
import com.covey.middleware.AuthMiddleware;
import com.covey.services.WeeklyJobService;
import com.covey.util.ApiGatewayUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class WeeklyJobHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
  private final WeeklyJobService weeklyJobService;
  private final AuthMiddleware authMiddleware;
  private final Gson gson;

  public WeeklyJobHandler() throws Exception {
    String apiKey = System.getenv("GOOGLE_PLACES_API_KEY");
    GooglePlacesClient placesClient = apiKey != null && !apiKey.isEmpty()
      ? new GooglePlacesClient(apiKey)
      : null;

    this.weeklyJobService = new WeeklyJobService(placesClient);
    this.gson = new Gson();

    FirebaseConfig.initialize();
    FirebaseAuth firebaseAuth = FirebaseConfig.getAuth();
    this.authMiddleware = new AuthMiddleware(firebaseAuth);
  }

  @Override
  public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
    context.getLogger().log("Weekly job request received");

    // Require Bearer token authentication
    String authHeader = ApiGatewayUtil.getAuthorizationHeader(event);
    Optional<String> userId = authMiddleware.validateToken(authHeader);
    if (userId.isEmpty()) {
      context.getLogger().log("Weekly job request rejected: missing or invalid authentication");
      Map<String, Object> response = new HashMap<>();
      response.put("statusCode", 401);
      response.put("body", gson.toJson(Map.of("error", "Unauthorized")));
      return response;
    }

    context.getLogger().log("Weekly job starting for authenticated user: " + userId.get());
    return runJob(context);
  }

  public Map<String, Object> handleScheduledEvent(Context context) {
    context.getLogger().log("Weekly selection job triggered by EventBridge");
    return runJob(context);
  }

  private Map<String, Object> runJob(Context context) {
    try {
      weeklyJobService.executeWeeklyJob();

      Map<String, Object> response = new HashMap<>();
      response.put("statusCode", 200);
      response.put("body", gson.toJson(Map.of("message", "Weekly job completed successfully")));
      return response;
    } catch (Exception e) {
      context.getLogger().log("Weekly job failed: " + e.getMessage());
      e.printStackTrace();

      Map<String, Object> response = new HashMap<>();
      response.put("statusCode", 500);
      response.put("body", gson.toJson(Map.of("error", e.getMessage())));
      return response;
    }
  }
}
