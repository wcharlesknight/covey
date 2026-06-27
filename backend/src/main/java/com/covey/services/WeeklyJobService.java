package com.covey.services;

import com.covey.integrations.GooglePlacesClient;
import com.covey.models.Invite;
import com.covey.models.User;
import com.covey.models.WeeklySpot;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class WeeklyJobService {
  private static final String USERS_COLLECTION = "users";
  private static final String WEEKLY_SPOTS_COLLECTION = "weeklySpots";
  private static final String INVITES_COLLECTION = "invites";

  private final GooglePlacesClient placesClient;

  // City coordinates (Seattle and Tacoma)
  private static final Map<String, double[]> CITY_COORDS = new HashMap<>();

  static {
    CITY_COORDS.put("Seattle", new double[]{47.6062, -122.3321});
    CITY_COORDS.put("Tacoma", new double[]{47.2529, -122.4443});
  }

  public WeeklyJobService(GooglePlacesClient placesClient) {
    this.placesClient = placesClient;
  }

  public void executeWeeklyJob() throws ExecutionException, InterruptedException, Exception {
    for (String city : CITY_COORDS.keySet()) {
      double[] coords = CITY_COORDS.get(city);
      executeForCity(city, coords[0], coords[1]);
    }
  }

  private void executeForCity(String city, double latitude, double longitude)
      throws Exception, ExecutionException, InterruptedException {
    List<WeeklySpot> venues = placesClient.searchVenues(city, latitude, longitude);

    if (venues.isEmpty()) {
      throw new Exception("No eligible venues found for " + city);
    }

    WeeklySpot selectedSpot = venues.get(0);
    selectedSpot.setId(UUID.randomUUID().toString());

    saveWeeklySpot(selectedSpot);

    List<User> usersInCity = getUsersByCity(city);

    for (User user : usersInCity) {
      Invite invite = new Invite(user.getUid(), selectedSpot.getId(), city);
      invite.setId(UUID.randomUUID().toString());
      saveInvite(invite);
    }
  }

  private void saveWeeklySpot(WeeklySpot spot) throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();
    db.collection(WEEKLY_SPOTS_COLLECTION).document(spot.getId()).set(spot).get();
  }

  private void saveInvite(Invite invite) throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();
    db.collection(INVITES_COLLECTION).document(invite.getId()).set(invite).get();
  }

  private List<User> getUsersByCity(String city)
      throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();
    Query query = db.collection(USERS_COLLECTION).whereEqualTo("city", city);

    ApiFuture<QuerySnapshot> future = query.get();
    QuerySnapshot snapshot = future.get();

    List<User> users = new ArrayList<>();
    for (var doc : snapshot.getDocuments()) {
      User user = doc.toObject(User.class);
      if (user != null) {
        user.setUid(doc.getId());
        users.add(user);
      }
    }

    return users;
  }
}
