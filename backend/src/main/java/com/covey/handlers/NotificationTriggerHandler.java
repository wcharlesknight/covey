package com.covey.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.covey.config.FirebaseConfig;
import com.covey.middleware.AuthMiddleware;
import com.covey.models.ScheduledNotification;
import com.covey.services.NotificationService;
import com.covey.util.ApiGatewayUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manual trigger for testing notifications locally.
 *
 * Endpoint: POST /notifications/send-test
 * Body: { "userId": "user123", "weekId": "2026-W30", "city": "Seattle", "channel": "FCM" }
 * Auth: Bearer token required
 *
 * Sends notification immediately for testing (dev/QA only).
 */
public class NotificationTriggerHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
  private final NotificationService notificationService;
  private final AuthMiddleware authMiddleware;
  private final Gson gson;

  public NotificationTriggerHandler() throws Exception {
    this.notificationService = new NotificationService();
    this.gson = new Gson();

    FirebaseConfig.initialize();
    FirebaseAuth firebaseAuth = FirebaseConfig.getAuth();
    this.authMiddleware = new AuthMiddleware(firebaseAuth);
  }

  @Override
  public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
    context.getLogger().log("Notification trigger request received");

    // Require Bearer token authentication
    String authHeader = ApiGatewayUtil.getAuthorizationHeader(event);
    Optional<String> userId = authMiddleware.validateToken(authHeader);
    if (userId.isEmpty()) {
      context.getLogger().log("Notification trigger rejected: missing or invalid authentication");
      return errorResponse(401, "Unauthorized");
    }

    try {
      // Parse request body
      String body = (String) event.get("body");
      if (body == null) {
        return errorResponse(400, "Request body required");
      }

      @SuppressWarnings("unchecked")
      Map<String, String> params = gson.fromJson(body, Map.class);

      String recipientUserId = params.get("userId");
      String weekId = params.get("weekId");
      String city = params.get("city");
      String channelStr = params.get("channel");

      if (recipientUserId == null || weekId == null || city == null || channelStr == null) {
        return errorResponse(400, "Missing required fields: userId, weekId, city, channel");
      }

      // Parse channel enum
      ScheduledNotification.Channel channel;
      try {
        channel = ScheduledNotification.Channel.valueOf(channelStr);
      } catch (IllegalArgumentException e) {
        return errorResponse(400, "Invalid channel: " + channelStr + ". Use FCM or EMAIL");
      }

      // Create test notification
      ScheduledNotification notification = new ScheduledNotification(
          recipientUserId,
          weekId,
          city,
          channel,
          System.currentTimeMillis()  // Send immediately
      );
      notification.setId(ScheduledNotification.generateId(recipientUserId, weekId, channel));

      // Send notification
      context.getLogger().log("Sending test notification to user: " + recipientUserId);
      boolean sent = sendNotificationDirect(notification);

      if (sent) {
        return successResponse(200, "Notification sent successfully", Map.of(
            "userId", recipientUserId,
            "weekId", weekId,
            "city", city,
            "channel", channelStr));
      } else {
        return errorResponse(500, "Failed to send notification");
      }

    } catch (Exception e) {
      context.getLogger().log("Notification trigger failed: " + e.getMessage());
      e.printStackTrace();
      return errorResponse(500, "Error: " + e.getMessage());
    }
  }

  /**
   * Send notification via FCM and/or SES based on channel.
   */
  private boolean sendNotificationDirect(ScheduledNotification notification) throws Exception {
    try {
      if (ScheduledNotification.Channel.FCM.equals(notification.getChannel())) {
        notificationService.sendPushNotificationDirect(notification);
      } else if (ScheduledNotification.Channel.EMAIL.equals(notification.getChannel())) {
        notificationService.sendEmailNotificationDirect(notification);
      }
      return true;
    } catch (Exception e) {
      System.err.println("Error sending notification: " + e.getMessage());
      throw e;
    }
  }

  private Map<String, Object> successResponse(int statusCode, String message,
      Map<String, ?> data) {
    Map<String, Object> response = new HashMap<>();
    response.put("statusCode", statusCode);
    response.put("body", gson.toJson(Map.of(
        "message", message,
        "data", data)));
    return response;
  }

  private Map<String, Object> errorResponse(int statusCode, String error) {
    Map<String, Object> response = new HashMap<>();
    response.put("statusCode", statusCode);
    response.put("body", gson.toJson(Map.of("error", error)));
    return response;
  }
}
