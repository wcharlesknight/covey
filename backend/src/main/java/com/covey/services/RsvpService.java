package com.covey.services;

import com.covey.models.Invite;
import com.covey.models.Invite.Status;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class RsvpService {
  private static final String INVITES_COLLECTION = "invites";

  public Optional<Invite> getInvite(String inviteId) throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();
    DocumentReference docRef = db.collection(INVITES_COLLECTION).document(inviteId);
    ApiFuture<DocumentSnapshot> future = docRef.get();
    DocumentSnapshot document = future.get();

    if (document.exists()) {
      Invite invite = document.toObject(Invite.class);
      if (invite != null) {
        invite.setId(document.getId());
      }
      return Optional.ofNullable(invite);
    } else {
      return Optional.empty();
    }
  }

  public void updateInviteStatus(String inviteId, Status newStatus)
      throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();
    DocumentReference docRef = db.collection(INVITES_COLLECTION).document(inviteId);

    docRef.update("status", newStatus, "updatedAt", System.currentTimeMillis()).get();
  }

  public Optional<Invite> rsvp(String inviteId, String userId, Status newStatus)
      throws ExecutionException, InterruptedException, IllegalAccessException {
    Optional<Invite> inviteOptional = getInvite(inviteId);

    if (!inviteOptional.isPresent()) {
      return Optional.empty();
    }

    Invite invite = inviteOptional.get();

    if (!invite.getUserId().equals(userId)) {
      throw new IllegalAccessException("User does not own this invite");
    }

    if (!isValidStatusTransition(invite.getStatus(), newStatus)) {
      throw new IllegalArgumentException(
          "Invalid status transition from " + invite.getStatus() + " to " + newStatus);
    }

    updateInviteStatus(inviteId, newStatus);

    invite.setStatus(newStatus);
    return Optional.of(invite);
  }

  private boolean isValidStatusTransition(Status from, Status to) {
    return true;
  }
}
