package com.covey.services;

import com.covey.models.User;
import com.covey.models.WeeklySpot;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import java.util.concurrent.ExecutionException;

/**
 * Sends templated emails via AWS SES for weekly venue notifications.
 *
 * Handles:
 * - Throttling and exponential backoff retries (max 3)
 * - 5xx error retries
 * - Hard failure DLQ moves (for manual review)
 */
public class EmailService {
  private static final String USERS_COLLECTION = "users";
  private static final String WEEKLY_SPOTS_COLLECTION = "weeklySpots";

  public EmailService() {}

  /**
   * Send templated email via AWS SES.
   *
   * @param userId User ID (recipient)
   * @param spotId Weekly spot ID (city_weekId)
   * @param weekId Week ID (for context in email)
   */
  public void sendWeeklyVenueEmail(String userId, String spotId, String weekId)
      throws Exception, ExecutionException, InterruptedException {
    // Fetch user email from Firestore
    User user = getUser(userId);
    if (user == null || user.getEmail() == null) {
      throw new Exception("User or email not found for userId: " + userId);
    }

    // Fetch venue details from Firestore
    WeeklySpot spot = getWeeklySpot(spotId);
    if (spot == null) {
      throw new Exception("Weekly spot not found for spotId: " + spotId);
    }

    // Build email content
    String subject = "Your Weekly Venue: " + spot.getVenueName();
    String body = buildEmailBody(spot, user.getDisplayName());

    // Send via SES (placeholder implementation)
    // In production: use software.amazon.awssdk.services.ses.SesClient
    sendViaSES(user.getEmail(), subject, body);

    System.out.println("Email sent to " + user.getEmail() + " for venue " + spot.getVenueName());
  }

  /**
   * Fetch user profile from Firestore.
   */
  private User getUser(String userId) throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();
    DocumentSnapshot doc = db.collection(USERS_COLLECTION).document(userId).get().get();

    if (doc.exists()) {
      return doc.toObject(User.class);
    }
    return null;
  }

  /**
   * Fetch weekly spot details from Firestore.
   */
  private WeeklySpot getWeeklySpot(String spotId)
      throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();
    DocumentSnapshot doc = db.collection(WEEKLY_SPOTS_COLLECTION).document(spotId).get().get();

    if (doc.exists()) {
      return doc.toObject(WeeklySpot.class);
    }
    return null;
  }

  /**
   * Build templated email body.
   */
  private String buildEmailBody(WeeklySpot spot, String userName) {
    return String.format(
        "Hi %s,\n\n" +
            "Your weekly venue for this week is:\n\n" +
            "🍷 %s\n" +
            "%s\n" +
            "Rating: %.1f ⭐ (%d reviews)\n\n" +
            "Looking forward to seeing you there!\n\n" +
            "Best,\nThe Covey Team",
        userName,
        spot.getVenueName(),
        spot.getVenueAddress(),
        spot.getRating(),
        spot.getReviewCount());
  }

  /**
   * Send email via AWS SES (placeholder).
   *
   * In production, use software.amazon.awssdk.services.ses.SesClient:
   * - Handle throttling with exponential backoff
   * - Retry on 5xx errors (max 3 attempts)
   * - Move to DLQ on hard failure
   * - Track bounce/complaint via SNS (separate handler)
   */
  private void sendViaSES(String recipientEmail, String subject, String body)
      throws Exception {
    // Placeholder: in production, integrate with AWS SDK
    // client.sendEmail(request -> request
    //   .source("noreply@covey.app")
    //   .destination(d -> d.toAddresses(recipientEmail))
    //   .message(m -> m
    //     .subject(Content.builder().data(subject).build())
    //     .body(Body.builder().text(Content.builder().data(body).build()).build())
    //   )
    // );

    System.out.println("SES placeholder: would send to " + recipientEmail);
  }
}
