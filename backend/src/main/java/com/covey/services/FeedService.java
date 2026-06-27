package com.covey.services;

import com.covey.models.Invite;
import com.covey.models.WeeklySpot;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class FeedService {
  private static final String INVITES_COLLECTION = "invites";
  private static final String WEEKLY_SPOTS_COLLECTION = "weeklySpots";
  private static final long FOUR_WEEKS_MS = 4L * 7 * 24 * 60 * 60 * 1000;

  public List<Invite> getUserFeed(String uid, String city)
      throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();

    long fourWeeksAgo = System.currentTimeMillis() - FOUR_WEEKS_MS;

    Query query = db.collection(INVITES_COLLECTION)
        .whereEqualTo("userId", uid)
        .whereEqualTo("city", city)
        .whereGreaterThanOrEqualTo("createdAt", fourWeeksAgo)
        .orderBy("createdAt", Query.Direction.DESCENDING);

    ApiFuture<QuerySnapshot> future = query.get();
    QuerySnapshot snapshot = future.get();

    List<Invite> invites = new ArrayList<>();
    for (DocumentSnapshot doc : snapshot.getDocuments()) {
      Invite invite = doc.toObject(Invite.class);
      if (invite != null) {
        invite.setId(doc.getId());
        invites.add(invite);
      }
    }

    return invites;
  }

  public WeeklySpot getWeeklySpot(String weeklySpotId)
      throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();
    ApiFuture<DocumentSnapshot> future = db.collection(WEEKLY_SPOTS_COLLECTION)
        .document(weeklySpotId)
        .get();
    DocumentSnapshot doc = future.get();

    if (doc.exists()) {
      WeeklySpot spot = doc.toObject(WeeklySpot.class);
      if (spot != null) {
        spot.setId(doc.getId());
      }
      return spot;
    }

    return null;
  }

  public List<WeeklySpot> getWeeklySpots(List<String> spotIds)
      throws ExecutionException, InterruptedException {
    List<WeeklySpot> spots = new ArrayList<>();
    for (String spotId : spotIds) {
      WeeklySpot spot = getWeeklySpot(spotId);
      if (spot != null) {
        spots.add(spot);
      }
    }
    return spots;
  }
}
