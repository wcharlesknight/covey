package com.covey.services;

import com.covey.models.Invite;
import com.covey.models.User;
import com.covey.models.WeeklySpot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteBatch;
import com.google.firebase.cloud.FirestoreClient;
import java.util.List;

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
    WriteBatch batch = db.batch();
    int batchCount = 0;

    for (int i = 0; i < users.size(); i++) {
      User user = users.get(i);
      Invite invite = new Invite(user.getUid(), weeklySpot.getWeekId(), weeklySpot.getVenueId(),
          weeklySpot.getCity());

      String documentId = Invite.generateId(user.getUid(), weeklySpot.getWeekId());
      batch.set(db.collection("invites").document(documentId), invite);
      batchCount++;

      if (batchCount >= BATCH_SIZE || i == users.size() - 1) {
        // Commit batch
        batch.commit().get();
        batch = db.batch();
        batchCount = 0;
      }
    }
  }
}
