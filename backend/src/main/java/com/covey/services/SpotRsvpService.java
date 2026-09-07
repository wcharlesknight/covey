package com.covey.services;

import com.covey.models.Rsvp;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class SpotRsvpService {
  private static final String RSVPS_COLLECTION = "rsvps";

  public Rsvp upsert(String spotId, String userId, String city, String weekId, Rsvp.Status status)
      throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();
    String docId = Rsvp.generateId(spotId, userId);
    DocumentReference docRef = db.collection(RSVPS_COLLECTION).document(docId);

    DocumentSnapshot existing = docRef.get().get();
    long now = System.currentTimeMillis();

    Rsvp rsvp = new Rsvp(userId, spotId, city, weekId, status);
    if (existing.exists()) {
      rsvp.setCreatedAt(existing.getLong("createdAt") != null ? existing.getLong("createdAt") : now);
    }
    rsvp.setUpdatedAt(now);

    docRef.set(rsvp).get();
    return rsvp;
  }

  public List<Rsvp> getRsvpsForSpot(String spotId)
      throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();
    Query query = db.collection(RSVPS_COLLECTION).whereEqualTo("spotId", spotId);
    ApiFuture<QuerySnapshot> future = query.get();
    QuerySnapshot snapshot = future.get();

    List<Rsvp> rsvps = new ArrayList<>();
    for (DocumentSnapshot doc : snapshot.getDocuments()) {
      Rsvp rsvp = doc.toObject(Rsvp.class);
      if (rsvp != null) {
        rsvp.setId(doc.getId());
        rsvps.add(rsvp);
      }
    }
    return rsvps;
  }

  public Optional<Rsvp> getUserRsvp(String spotId, String userId)
      throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();
    String docId = Rsvp.generateId(spotId, userId);
    DocumentSnapshot doc = db.collection(RSVPS_COLLECTION).document(docId).get().get();

    if (doc.exists()) {
      Rsvp rsvp = doc.toObject(Rsvp.class);
      if (rsvp != null) {
        rsvp.setId(doc.getId());
      }
      return Optional.ofNullable(rsvp);
    }
    return Optional.empty();
  }
}
