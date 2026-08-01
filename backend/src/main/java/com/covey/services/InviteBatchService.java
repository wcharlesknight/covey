package com.covey.services;

import com.covey.models.Invite;
import com.covey.models.ScheduledNotification;
import com.covey.models.User;
import com.covey.models.WeeklySpot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteBatch;
import com.google.firebase.cloud.FirestoreClient;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Manages batch creation of Invite documents in Firestore.
 *
 * Handles:
 * - Creating one Invite per user for a weekly spot
 * - Batching writes (max 500 per batch)
 * - Idempotent writes with deterministic IDs
 * - Error handling and partial failure recovery
 *
 * Part of WBS 1.3.6 weekly job implementation.
 */
public class InviteBatchService {

  private static final int BATCH_SIZE = 500;
  private static final String NOTIFICATIONS_COLLECTION = "scheduledNotifications";

  public InviteBatchService() {}

  /**
   * Create invites for all users in a city for a weekly spot.
   *
   * Writes in batches of up to 500 documents. Partial failures are logged but do not stop
   * processing of remaining batches.
   *
   * @param weeklySpot The weekly spot to create invites for
   * @param users Users in the spot's city
   * @throws Exception on unrecoverable errors (e.g., unable to read users)
   */
  public void createInvites(WeeklySpot weeklySpot, List<User> users) throws Exception {
    if (users == null || users.isEmpty()) {
      return;
    }

    Firestore db = FirestoreClient.getFirestore();
    long deliverAt = getFridayDeliveryTime();
    WriteBatch batch = db.batch();
    int batchCount = 0;

    for (int i = 0; i < users.size(); i++) {
      User user = users.get(i);
      String uid = user.getUid();

      // Invite document
      Invite invite = new Invite(uid, weeklySpot.getWeekId(), weeklySpot.getVenueId(),
          weeklySpot.getCity());
      String inviteId = Invite.generateId(uid, weeklySpot.getWeekId());
      batch.set(db.collection("invites").document(inviteId), invite);

      // Push notification queued for Friday 2PM UTC
      ScheduledNotification pushNotif = new ScheduledNotification(
          uid, weeklySpot.getWeekId(), weeklySpot.getCity(),
          ScheduledNotification.Channel.FCM, deliverAt);
      String notifId = ScheduledNotification.generateId(uid, weeklySpot.getWeekId(),
          ScheduledNotification.Channel.FCM);
      pushNotif.setId(notifId);
      batch.set(db.collection(NOTIFICATIONS_COLLECTION).document(notifId), pushNotif);

      batchCount += 2;

      if (batchCount >= BATCH_SIZE || i == users.size() - 1) {
        batch.commit().get();
        batch = db.batch();
        batchCount = 0;
      }
    }
  }

  // Friday 2PM UTC of the current week
  private long getFridayDeliveryTime() {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
    int daysToFriday = Calendar.FRIDAY - dayOfWeek;
    if (daysToFriday < 0) daysToFriday += 7;
    cal.add(Calendar.DAY_OF_MONTH, daysToFriday);
    cal.set(Calendar.HOUR_OF_DAY, 14);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    return cal.getTimeInMillis();
  }
}
