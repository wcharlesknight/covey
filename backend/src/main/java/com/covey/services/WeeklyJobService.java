package com.covey.services;

import com.covey.integrations.GooglePlacesClient;
import com.covey.models.User;
import com.covey.models.VenueExclusion;
import com.covey.models.WeeklyJobRun;
import com.covey.models.WeeklySpot;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class WeeklyJobService {
  private static final String USERS_COLLECTION = "users";
  private static final String WEEKLY_SPOTS_COLLECTION = "weeklySpots";
  private static final String VENUE_EXCLUSIONS_COLLECTION = "venueExclusions";
  private static final String JOB_RUNS_COLLECTION = "weeklyJobRuns";

  private final GooglePlacesClient placesClient;
  private final VenueRotationService rotationService;
  private final InviteBatchService inviteBatchService;

  private static final Map<String, double[]> CITY_COORDS = new HashMap<>();

  static {
    CITY_COORDS.put("Seattle", new double[]{47.6062, -122.3321});
    CITY_COORDS.put("Tacoma", new double[]{47.2529, -122.4443});
    CITY_COORDS.put("Bainbridge Island", new double[]{47.6262, -122.5209});
  }

  public WeeklyJobService(GooglePlacesClient placesClient) {
    this.placesClient = placesClient;
    this.rotationService = new VenueRotationService();
    this.inviteBatchService = new InviteBatchService();
  }

  public void executeWeeklyJob() throws ExecutionException, InterruptedException, Exception {
    String weekId = getCurrentWeekId();
    long weekStartDate = getWeekStartDate();

    // Check if job already completed for this week (idempotency guard)
    if (jobAlreadyCompleted(weekId)) {
      System.out.println("Weekly job for " + weekId + " already completed, skipping");
      return;
    }

    // Create job run record
    WeeklyJobRun jobRun = new WeeklyJobRun();
    jobRun.setId(UUID.randomUUID().toString());
    jobRun.setWeekId(weekId);
    jobRun.setStartedAt(System.currentTimeMillis());
    jobRun.setStatus("IN_PROGRESS");

    Map<String, String> cityResults = new HashMap<>();
    int citiesProcessed = 0;
    int citiesSkipped = 0;
    int citiesErrored = 0;

    for (String city : CITY_COORDS.keySet()) {
      try {
        double[] coords = CITY_COORDS.get(city);
        executeForCity(city, coords[0], coords[1], weekId, weekStartDate);
        cityResults.put(city, "SUCCESS");
        citiesProcessed++;
      } catch (Exception e) {
        // Skip city on error, continue to next
        System.err.println("Error processing city " + city + ": " + e.getMessage());
        cityResults.put(city, "ERROR: " + e.getMessage());
        citiesErrored++;
      }
    }

    // Update job run record with results
    jobRun.setCompletedAt(System.currentTimeMillis());
    jobRun.setStatus("COMPLETED");
    jobRun.setCitiesProcessed(citiesProcessed);
    jobRun.setCitiesSkipped(citiesSkipped);
    jobRun.setCitiesErrored(citiesErrored);

    saveJobRun(jobRun);
  }

  private boolean jobAlreadyCompleted(String weekId)
      throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();
    Query query = db.collection(JOB_RUNS_COLLECTION)
        .whereEqualTo("weekId", weekId)
        .whereEqualTo("status", "COMPLETED");

    ApiFuture<QuerySnapshot> future = query.get();
    QuerySnapshot snapshot = future.get();

    return !snapshot.isEmpty();
  }

  private void saveJobRun(WeeklyJobRun jobRun)
      throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();
    db.collection(JOB_RUNS_COLLECTION).document(jobRun.getId()).set(jobRun).get();
  }

  private void executeForCity(String city, double latitude, double longitude, String weekId,
      long weekStartDate) throws Exception, ExecutionException, InterruptedException {
    // 1. Get venue candidates from Google Places API
    List<WeeklySpot> venues = placesClient.searchVenues(city, latitude, longitude);

    if (venues.isEmpty()) {
      // Skip city if no venues found
      System.out.println("No eligible venues found for " + city + ", skipping");
      return;
    }

    // 2. Filter out excluded venues (12-week rotation)
    Set<String> exclusions = getExclusionSet(city);
    List<WeeklySpot> eligible = new ArrayList<>();
    for (WeeklySpot venue : venues) {
      if (!exclusions.contains(venue.getVenueId())) {
        eligible.add(venue);
      }
    }

    if (eligible.isEmpty()) {
      // All venues excluded, skip city
      System.out.println("All venues excluded for " + city + ", skipping");
      return;
    }

    // 3. Select top-ranked venue
    WeeklySpot selectedSpot = placesClient.selectTopVenue(eligible);
    selectedSpot.setId(WeeklySpot.generateId(city, weekId));
    selectedSpot.setWeekId(weekId);

    // 4. Save WeeklySpot to Firestore
    saveWeeklySpot(selectedSpot);

    // 5. Record this venue in exclusion list for future weeks
    recordVenueExclusion(city, weekId, selectedSpot.getVenueId(), weekStartDate);

    // 6. Get users in this city
    List<User> usersInCity = getUsersByCity(city);

    // 7. Create and batch write invites for all users
    if (!usersInCity.isEmpty()) {
      inviteBatchService.createInvites(selectedSpot, usersInCity);
    }
  }

  private Set<String> getExclusionSet(String city)
      throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();
    // Query venue exclusions for this city
    Query query = db.collection(VENUE_EXCLUSIONS_COLLECTION).whereEqualTo("city", city);

    ApiFuture<QuerySnapshot> future = query.get();
    QuerySnapshot snapshot = future.get();

    List<VenueExclusion> exclusions = new ArrayList<>();
    for (var doc : snapshot.getDocuments()) {
      VenueExclusion exclusion = doc.toObject(VenueExclusion.class);
      if (exclusion != null) {
        exclusions.add(exclusion);
      }
    }

    return rotationService.buildExclusionSet(exclusions);
  }

  private void saveWeeklySpot(WeeklySpot spot) throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();
    db.collection(WEEKLY_SPOTS_COLLECTION).document(spot.getId()).set(spot).get();
  }

  private void recordVenueExclusion(String city, String weekId, String venueId,
      long weekStartDate) throws ExecutionException, InterruptedException {
    Firestore db = FirestoreClient.getFirestore();
    VenueExclusion exclusion = new VenueExclusion();
    exclusion.setCity(city);
    exclusion.setWeekId(weekId);
    exclusion.setVenueIds(List.of(venueId));
    exclusion.setUpdatedAt(System.currentTimeMillis());

    String documentId = VenueExclusion.generateId(city, weekId);
    db.collection(VENUE_EXCLUSIONS_COLLECTION).document(documentId).set(exclusion).get();
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

  private String getCurrentWeekId() {
    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-'W'ww");
    sdf.setCalendar(calendar);
    return sdf.format(calendar.getTime());
  }

  private long getWeekStartDate() {
    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
    // Set to Thursday of current week
    int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
    int daysToThursday = Calendar.THURSDAY - dayOfWeek;
    if (daysToThursday > 0) {
      daysToThursday -= 7;
    }
    calendar.add(Calendar.DAY_OF_MONTH, daysToThursday);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar.getTimeInMillis();
  }
}
