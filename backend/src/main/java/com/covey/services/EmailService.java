package com.covey.services;

import com.covey.models.User;
import com.covey.models.WeeklySpot;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import java.util.concurrent.ExecutionException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

public class EmailService {
  private static final String USERS_COLLECTION = "users";
  private static final String WEEKLY_SPOTS_COLLECTION = "weeklySpots";
  private static final String SENDER_EMAIL = System.getenv("SES_SENDER_EMAIL") != null
      ? System.getenv("SES_SENDER_EMAIL")
      : "williamchknight@gmail.com";
  private static final int MAX_RETRIES = 3;

  private final SesClient sesClient;

  public EmailService() {
    this.sesClient = SesClient.builder()
        .region(Region.US_WEST_2)
        .build();
  }

  public void sendWeeklyVenueEmail(String userId, String spotId, String weekId)
      throws Exception, ExecutionException, InterruptedException {
    User user = getUser(userId);
    if (user == null || user.getEmail() == null) {
      throw new Exception("User or email not found for userId: " + userId);
    }

    WeeklySpot spot = getWeeklySpot(spotId);
    if (spot == null) {
      throw new Exception("Weekly spot not found for spotId: " + spotId);
    }

    String name = user.getDisplayName() != null ? user.getDisplayName() : "there";
    String subject = "This week's spot: " + spot.getVenueName();
    String textBody = buildTextBody(spot, name);
    String htmlBody = buildHtmlBody(spot, name);

    sendWithRetry(user.getEmail(), subject, textBody, htmlBody);
    System.out.println("Email sent to " + user.getEmail() + " for venue " + spot.getVenueName());
  }

  private void sendWithRetry(String to, String subject, String textBody, String htmlBody)
      throws Exception {
    int attempts = 0;
    long backoffMs = 500;

    while (attempts < MAX_RETRIES) {
      try {
        SendEmailRequest request = SendEmailRequest.builder()
            .source(SENDER_EMAIL)
            .destination(Destination.builder().toAddresses(to).build())
            .message(Message.builder()
                .subject(Content.builder().data(subject).charset("UTF-8").build())
                .body(Body.builder()
                    .text(Content.builder().data(textBody).charset("UTF-8").build())
                    .html(Content.builder().data(htmlBody).charset("UTF-8").build())
                    .build())
                .build())
            .build();

        sesClient.sendEmail(request);
        return;
      } catch (SesException e) {
        attempts++;
        String code = e.awsErrorDetails().errorCode();
        // Throttling and server errors are retryable
        boolean retryable = code.equals("Throttling") || code.equals("ServiceUnavailable")
            || e.statusCode() >= 500;
        if (!retryable || attempts >= MAX_RETRIES) {
          throw new Exception("SES send failed (" + code + "): " + e.getMessage(), e);
        }
        System.err.println("SES throttled, retrying in " + backoffMs + "ms (attempt " + attempts + ")");
        Thread.sleep(backoffMs);
        backoffMs *= 2;
      }
    }
  }

  private String buildTextBody(WeeklySpot spot, String name) {
    String deepLink = buildDeepLink(spot);
    return String.format(
        "Hi %s,\n\n" +
        "This week's spot for %s is:\n\n" +
        "%s\n" +
        "%s\n" +
        "Rating: %.1f stars (%d reviews)\n\n" +
        "Open in Covey: %s\n\n" +
        "— The Covey Team",
        name, spot.getCity(), spot.getVenueName(), spot.getVenueAddress(),
        spot.getRating(), spot.getReviewCount(), deepLink);
  }

  private String buildHtmlBody(WeeklySpot spot, String name) {
    String deepLink = buildDeepLink(spot);
    return String.format(
        "<!DOCTYPE html><html><body style='font-family:sans-serif;max-width:480px;margin:0 auto;padding:24px;'>" +
        "<h2 style='color:#6B4CE6;'>This week's spot 🍻</h2>" +
        "<p>Hi %s,</p>" +
        "<p>This week's spot for <strong>%s</strong> is:</p>" +
        "<div style='background:#f9fafb;border-left:4px solid #6B4CE6;border-radius:8px;padding:16px;margin:16px 0;'>" +
        "<h3 style='margin:0 0 4px;color:#1F2937;'>%s</h3>" +
        "<p style='margin:0 0 8px;color:#6B7280;'>%s</p>" +
        "<p style='margin:0;color:#6B7280;'>⭐ %.1f · %d reviews</p>" +
        "</div>" +
        "<a href='%s' style='display:inline-block;background:#6B4CE6;color:#fff;font-weight:600;" +
        "text-decoration:none;padding:14px 28px;border-radius:10px;font-size:16px;margin:8px 0;'>" +
        "See this week's spot →</a>" +
        "<p style='color:#9CA3AF;font-size:12px;margin-top:32px;'>— The Covey Team</p>" +
        "</body></html>",
        name, spot.getCity(), spot.getVenueName(), spot.getVenueAddress(),
        spot.getRating(), spot.getReviewCount(), deepLink);
  }

  private String buildDeepLink(WeeklySpot spot) {
    return String.format("covey://spot?spotId=%s_%s&city=%s&weekId=%s",
        spot.getCity(), spot.getWeekId(), spot.getCity(), spot.getWeekId());
  }

  private User getUser(String userId) throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();
    DocumentSnapshot doc = db.collection(USERS_COLLECTION).document(userId).get().get();
    return doc.exists() ? doc.toObject(User.class) : null;
  }

  private WeeklySpot getWeeklySpot(String spotId) throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();
    DocumentSnapshot doc = db.collection(WEEKLY_SPOTS_COLLECTION).document(spotId).get().get();
    return doc.exists() ? doc.toObject(WeeklySpot.class) : null;
  }
}
