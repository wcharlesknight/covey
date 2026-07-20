package com.covey.integrations;

import com.covey.models.WeeklySpot;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GooglePlacesClient {
  private static final String BASE_URL = "https://places.googleapis.com/v1/places:searchNearby";
  private static final int RADIUS = 5000;
  private static final String INCLUDED_TYPE = "bar";
  private static final double MIN_RATING = 4.0;
  private static final int MIN_REVIEWS = 50;
  private static final String FIELD_MASK =
      "places.id,places.displayName,places.formattedAddress,places.rating,places.userRatingCount,places.location";

  private final String apiKey;

  public GooglePlacesClient(String apiKey) {
    this.apiKey = apiKey;
  }

  public List<WeeklySpot> searchVenues(String city, double latitude, double longitude)
      throws Exception {
    String requestBody = buildRequestBody(latitude, longitude);
    String response = postJson(BASE_URL, requestBody);
    JsonObject json = JsonParser.parseString(response).getAsJsonObject();

    if (json.has("error")) {
      JsonObject error = json.getAsJsonObject("error");
      String status = error.has("status") ? error.get("status").getAsString() : "UNKNOWN";
      String message = error.has("message") ? error.get("message").getAsString() : "no detail";
      throw new Exception("Google Places API error: " + status + " — " + message);
    }

    List<WeeklySpot> results = new ArrayList<>();

    if (!json.has("places")) {
      return results;
    }

    JsonArray places = json.getAsJsonArray("places");
    for (int i = 0; i < places.size(); i++) {
      JsonObject place = places.get(i).getAsJsonObject();

      double rating = place.has("rating") ? place.get("rating").getAsDouble() : 0.0;
      int reviewCount = place.has("userRatingCount")
          ? place.get("userRatingCount").getAsInt()
          : 0;

      if (rating >= MIN_RATING && reviewCount >= MIN_REVIEWS) {
        String name = place.getAsJsonObject("displayName").get("text").getAsString();
        String address = place.has("formattedAddress")
            ? place.get("formattedAddress").getAsString()
            : "";
        String placeId = place.get("id").getAsString();

        WeeklySpot spot = new WeeklySpot(city, name, address, placeId, rating, reviewCount,
            System.currentTimeMillis());
        results.add(spot);
      }
    }

    results.sort((a, b) -> {
      int ratingCompare = Double.compare(b.getRating(), a.getRating());
      if (ratingCompare != 0) return ratingCompare;
      return Integer.compare(b.getReviewCount(), a.getReviewCount());
    });

    return results;
  }

  public WeeklySpot selectTopVenue(List<WeeklySpot> venues) {
    if (venues == null || venues.isEmpty()) return null;
    venues.sort((a, b) -> {
      int ratingCompare = Double.compare(b.getRating(), a.getRating());
      if (ratingCompare != 0) return ratingCompare;
      return Integer.compare(b.getReviewCount(), a.getReviewCount());
    });
    return venues.get(0);
  }

  private String buildRequestBody(double latitude, double longitude) {
    return String.format(
        "{\"includedTypes\":[\"%s\"]," +
        "\"maxResultCount\":20," +
        "\"locationRestriction\":{\"circle\":{\"center\":{\"latitude\":%f,\"longitude\":%f},\"radius\":%d}}}",
        INCLUDED_TYPE, latitude, longitude, RADIUS);
  }

  private String postJson(String urlString, String body) throws Exception {
    URL url = new URL(urlString);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setRequestProperty("X-Goog-Api-Key", apiKey);
    conn.setRequestProperty("X-Goog-FieldMask", FIELD_MASK);
    conn.setDoOutput(true);

    try (OutputStream os = conn.getOutputStream()) {
      os.write(body.getBytes(StandardCharsets.UTF_8));
    }

    int statusCode = conn.getResponseCode();
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        statusCode >= 400 ? conn.getErrorStream() : conn.getInputStream()));
    StringBuilder response = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      response.append(line);
    }
    reader.close();
    return response.toString();
  }
}
