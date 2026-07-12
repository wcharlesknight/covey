package com.covey.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.covey.config.FirebaseConfig;
import com.covey.services.NotificationService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;

/**
 * Lambda handler for Friday 9am notification delivery.
 *
 * Reads scheduled_notifications queue and sends queued FCM + SES messages.
 * Marks tasks SENT/FAILED, deactivates dead tokens, DLQs hard failures.
 */
public class NotificationDispatchHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
  private final NotificationService notificationService;
  private final Gson gson;

  public NotificationDispatchHandler() throws Exception {
    this.notificationService = new NotificationService();
    this.gson = new Gson();

    FirebaseConfig.initialize();
    FirebaseAuth firebaseAuth = FirebaseConfig.getAuth();
  }

  @Override
  public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
    context.getLogger().log("Notification dispatch run started");

    try {
      int notificationsSent = notificationService.dispatchScheduledNotifications();

      Map<String, Object> response = new HashMap<>();
      response.put("statusCode", 200);
      response.put("body", gson.toJson(
          Map.of("message", "Notification dispatch completed",
              "notificationsSent", notificationsSent)));
      return response;
    } catch (Exception e) {
      context.getLogger().log("Notification dispatch failed: " + e.getMessage());
      e.printStackTrace();

      Map<String, Object> response = new HashMap<>();
      response.put("statusCode", 500);
      response.put("body", gson.toJson(Map.of("error", e.getMessage())));
      return response;
    }
  }
}
