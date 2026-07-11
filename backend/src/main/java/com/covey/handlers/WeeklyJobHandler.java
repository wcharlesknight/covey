package com.covey.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.covey.config.FirebaseConfig;
import com.covey.integrations.GooglePlacesClient;
import com.covey.services.WeeklyJobService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;

public class WeeklyJobHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
  private final WeeklyJobService weeklyJobService;
  private final Gson gson;

  public WeeklyJobHandler() throws Exception {
    String apiKey = System.getenv("GOOGLE_PLACES_API_KEY");
    GooglePlacesClient placesClient = apiKey != null && !apiKey.isEmpty()
      ? new GooglePlacesClient(apiKey)
      : null;

    this.weeklyJobService = new WeeklyJobService(placesClient);
    this.gson = new Gson();

    FirebaseConfig.initialize();
  }

  @Override
  public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
    context.getLogger().log("Weekly job starting");

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
