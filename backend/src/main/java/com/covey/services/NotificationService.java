package com.covey.services;

import com.covey.models.ScheduledNotification;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Orchestrates notification delivery via FCM (push) and SES (email).
 *
 * Reads scheduled_notifications queue and:
 * 1. Sends FCM push notifications (batch up to 500 tokens per user)
 * 2. Sends SES templated emails
 * 3. Marks tasks SENT/FAILED
 * 4. Deactivates unregistered tokens
 * 5. Moves hard failures to DLQ
 */
public class NotificationService {
  private static final String NOTIFICATIONS_COLLECTION = "scheduledNotifications";

  private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

  private final PushTokenService pushTokenService;
  private final EmailService emailService;
  private final HttpClient httpClient;
  private final Gson gson;

  public NotificationService() {
    this.pushTokenService = new PushTokenService();
    this.emailService = new EmailService();
    this.httpClient = HttpClient.newHttpClient();
    this.gson = new Gson();
  }

  /**
   * Main dispatch loop: drain scheduled_notifications queue.
   *
   * @return number of notifications successfully sent
   */
  public int dispatchScheduledNotifications()
      throws ExecutionException, InterruptedException, Exception {
    Firestore db = FirestoreClient.getFirestore();

    // Query for pending notifications (deliverAt <= now, status == PENDING)
    long now = System.currentTimeMillis();
    Query query = db.collection(NOTIFICATIONS_COLLECTION)
        .whereLessThanOrEqualTo("deliverAt", now)
        .whereEqualTo("status", "PENDING");

    ApiFuture<QuerySnapshot> future = query.get();
    QuerySnapshot snapshot = future.get();

    int sentCount = 0;

    for (DocumentSnapshot doc : snapshot.getDocuments()) {
      ScheduledNotification notification = doc.toObject(ScheduledNotification.class);
      if (notification != null) {
        try {
          boolean sent = sendNotification(notification);
          if (sent) {
            sentCount++;
            // Mark as SENT
            markNotificationSent(notification.getId());
          } else {
            // Mark as FAILED
            markNotificationFailed(notification.getId());
          }
        } catch (Exception e) {
          System.err.println("Error sending notification " + notification.getId() + ": " + e.getMessage());
          // Move to DLQ on hard failure
          markNotificationFailed(notification.getId());
        }
      }
    }

    return sentCount;
  }

  /**
   * Send a single notification via FCM and/or SES.
   *
   * @return true if sent successfully
   */
  private boolean sendNotification(ScheduledNotification notification)
      throws Exception, ExecutionException, InterruptedException {
    boolean success = false;

    // Send via FCM if channel is FCM
    if (ScheduledNotification.Channel.FCM.equals(notification.getChannel())) {
      try {
        sendPushNotification(notification);
        success = true;
      } catch (Exception e) {
        System.err.println("FCM send failed: " + e.getMessage());
      }
    }

    // Send via SES if channel is EMAIL
    if (ScheduledNotification.Channel.EMAIL.equals(notification.getChannel())) {
      try {
        sendEmailNotification(notification);
        success = true;
      } catch (Exception e) {
        System.err.println("SES send failed: " + e.getMessage());
      }
    }

    return success;
  }

  /**
   * Send push notification directly (for testing/manual triggers).
   */
  public void sendPushNotificationDirect(ScheduledNotification notification)
      throws ExecutionException, InterruptedException, Exception {
    sendPushNotification(notification);
  }

  /**
   * Send email notification directly (for testing/manual triggers).
   */
  public void sendEmailNotificationDirect(ScheduledNotification notification)
      throws Exception {
    sendEmailNotification(notification);
  }

  /**
   * Send push notification via Expo push API.
   */
  private void sendPushNotification(ScheduledNotification notification)
      throws ExecutionException, InterruptedException, Exception {
    List<String> tokens = pushTokenService.getActiveTokensByUser(notification.getUserId());

    if (tokens.isEmpty()) {
      System.out.println("No active tokens for user " + notification.getUserId());
      return;
    }

    String spotId = notification.getCity() + "_" + notification.getWeekId();

    JsonArray messages = new JsonArray();
    for (String token : tokens) {
      JsonObject msg = new JsonObject();
      msg.addProperty("to", token);
      msg.addProperty("title", "This week's spot is ready 🍻");
      msg.addProperty("body", "See where " + notification.getCity() + " is gathering this week.");
      msg.addProperty("sound", "default");
      JsonObject data = new JsonObject();
      data.addProperty("weekId", notification.getWeekId());
      data.addProperty("city", notification.getCity());
      data.addProperty("spotId", spotId);
      msg.add("data", data);
      messages.add(msg);
    }

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(EXPO_PUSH_URL))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(messages)))
        .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      throw new Exception("Expo push API returned " + response.statusCode() + ": " + response.body());
    }

    JsonObject responseBody = gson.fromJson(response.body(), JsonObject.class);
    JsonArray tickets = responseBody.getAsJsonArray("data");
    for (int i = 0; i < tickets.size(); i++) {
      JsonObject ticket = tickets.get(i).getAsJsonObject();
      String status = ticket.get("status").getAsString();
      if ("error".equals(status)) {
        String errorMsg = ticket.has("message") ? ticket.get("message").getAsString() : "unknown";
        String token = tokens.get(i);
        System.err.println("Expo push error for token " + token + ": " + errorMsg);
        if (errorMsg.contains("DeviceNotRegistered")) {
          pushTokenService.markTokenInactive(notification.getUserId(),
              java.security.MessageDigest.getInstance("SHA-256") != null ? hashToken(token) : token);
        }
      } else {
        System.out.println("Expo push sent to " + notification.getUserId()
            + ": " + (ticket.has("id") ? ticket.get("id").getAsString() : "ok"));
      }
    }
  }

  private String hashToken(String token) throws Exception {
    java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    StringBuilder hexString = new StringBuilder();
    for (byte b : hash) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) hexString.append('0');
      hexString.append(hex);
    }
    return hexString.toString();
  }

  /**
   * Send email notification via AWS SES.
   */
  private void sendEmailNotification(ScheduledNotification notification)
      throws Exception {
    // Lookup WeeklySpot to get venue details
    String spotId = notification.getCity() + "_" + notification.getWeekId();
    emailService.sendWeeklyVenueEmail(notification.getUserId(), spotId,
        notification.getWeekId());
  }

  /**
   * Mark notification as sent in Firestore.
   */
  private void markNotificationSent(String notificationId)
      throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();
    db.collection(NOTIFICATIONS_COLLECTION).document(notificationId)
        .update("status", "SENT", "sentAt", System.currentTimeMillis())
        .get();
  }

  /**
   * Mark notification as failed in Firestore (moves to DLQ for manual review).
   */
  private void markNotificationFailed(String notificationId)
      throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();
    db.collection(NOTIFICATIONS_COLLECTION).document(notificationId)
        .update("status", "FAILED", "failedAt", System.currentTimeMillis())
        .get();
  }
}
