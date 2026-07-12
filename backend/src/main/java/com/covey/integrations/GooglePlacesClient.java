package com.covey.integrations;

import com.covey.models.WeeklySpot;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GooglePlacesClient {
  private static final String BASE_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";
  private static final int RADIUS = 5000;
  private static final String TYPE = "bar";
  private static final double MIN_RATING = 4.0;
  private static final int MIN_REVIEWS = 50;

  private final String apiKey;

  public GooglePlacesClient(String apiKey) {
    this.apiKey = apiKey;
  }

  public List<WeeklySpot> searchVenues(String city, double latitude, double longitude)
      throws Exception {
    String url = String.format(
        "%s?location=%f,%f&radius=%d&type=%s&key=%s",
        BASE_URL, latitude, longitude, RADIUS, TYPE, apiKey);

    List<WeeklySpot> results = new ArrayList<>();
    String response = fetchUrl(url);
    JsonObject json = JsonParser.parseString(response).getAsJsonObject();

    if (!json.get("status").getAsString().equals("OK")) {
      throw new Exception("Google Places API error: " + json.get("status").getAsString());
    }

    JsonArray results_array = json.getAsJsonArray("results");
    for (int i = 0; i < results_array.size(); i++) {
      JsonObject place = results_array.get(i).getAsJsonObject();

      double rating = place.has("rating") ? place.get("rating").getAsDouble() : 0.0;
      int review_count = place.has("user_ratings_total")
          ? place.get("user_ratings_total").getAsInt()
          : 0;

      if (rating >= MIN_RATING && review_count >= MIN_REVIEWS) {
        String name = place.get("name").getAsString();
        String address = place.get("vicinity").getAsString();
        String placeId = place.get("place_id").getAsString();

        WeeklySpot spot = new WeeklySpot(city, name, address, placeId, rating, review_count,
            System.currentTimeMillis());
        results.add(spot);
      }
    }

    // Sort by rating DESC, then review count DESC (highest rated, most reviewed first)
    results.sort((a, b) -> {
      int ratingCompare = Double.compare(b.getRating(), a.getRating());
      if (ratingCompare != 0) {
        return ratingCompare;
      }
      return Integer.compare(b.getReviewCount(), a.getReviewCount());
    });

    return results;
  }

  /**
   * Select the top-ranked venue from a list of candidates.
   *
   * Ranking: highest rating first, review count as tiebreaker.
   */
  public WeeklySpot selectTopVenue(List<WeeklySpot> venues) {
    if (venues == null || venues.isEmpty()) {
      return null;
    }
    return venues.get(0);
  }

  private String fetchUrl(String urlString) throws Exception {
    URL url = new URL(urlString);
    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
    StringBuilder response = new StringBuilder();
    String inputLine;

    while ((inputLine = in.readLine()) != null) {
      response.append(inputLine);
    }

    in.close();
    return response.toString();
  }
}
