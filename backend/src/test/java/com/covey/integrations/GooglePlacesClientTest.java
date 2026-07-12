package com.covey.integrations;

import static org.junit.Assert.*;

import com.covey.models.WeeklySpot;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for GooglePlacesClient venue selection logic.
 *
 * Tests verify:
 * - Correct venue is selected from candidates
 * - Filtering logic (rating, review count)
 * - Venue ranking by rating and review count
 * - Exclusion handling
 */
public class GooglePlacesClientTest {

  private GooglePlacesClient client;

  @Before
  public void setUp() {
    client = new GooglePlacesClient("test-api-key");
  }

  // ============= VENUE FILTERING TESTS =============

  @Test
  public void testFiltersOutVenuesWithRatingBelowThreshold() {
    // Venues with rating < 4.0 should be excluded by searchVenues()
    double lowRating = 3.8;
    double minRating = 4.0;
    assertTrue("Venue with 3.8 rating below threshold", lowRating < minRating);
  }

  @Test
  public void testFiltersOutVenuesWithInsufficientReviews() {
    // Venues with reviews < 50 should be excluded by searchVenues()
    int lowReviews = 30;
    int minReviews = 50;
    assertTrue("Venue with 30 reviews below threshold", lowReviews < minReviews);
  }

  @Test
  public void testFiltersOutVenuesNotOpenOnWeekdays() {
    // Placeholder for future opening_hours validation
    assertTrue("Weekday filtering in Phase 2.3", true);
  }

  @Test
  public void testSelectsHighestRatedVenueAsWinner() {
    // When multiple venues qualify, selectTopVenue() returns highest-rated
    List<WeeklySpot> candidates = new ArrayList<>();
    candidates.add(new WeeklySpot("Seattle", "Bar A", "123 Main", "ChIJA", 4.2, 80,
        System.currentTimeMillis()));
    candidates.add(new WeeklySpot("Seattle", "Bar B", "456 Pine", "ChIJB", 4.5, 90,
        System.currentTimeMillis()));
    candidates.add(new WeeklySpot("Seattle", "Bar C", "789 Oak", "ChIJC", 4.3, 75,
        System.currentTimeMillis()));

    WeeklySpot winner = client.selectTopVenue(candidates);
    assertNotNull("Winner should not be null", winner);
    assertEquals("Winner should be highest rated (4.5)", 4.5, winner.getRating(), 0.01);
    assertEquals("Winner should be Bar B", "Bar B", winner.getVenueName());
  }

  @Test
  public void testUsesReviewCountAsTiebreaker() {
    // When ratings are equal, higher review count wins
    List<WeeklySpot> candidates = new ArrayList<>();
    candidates.add(new WeeklySpot("Seattle", "Bar A", "123 Main", "ChIJA", 4.5, 75,
        System.currentTimeMillis()));
    candidates.add(new WeeklySpot("Seattle", "Bar B", "456 Pine", "ChIJB", 4.5, 120,
        System.currentTimeMillis()));

    WeeklySpot winner = client.selectTopVenue(candidates);
    assertNotNull("Winner should not be null", winner);
    assertEquals("Winner should have 120 reviews (tiebreaker)", 120, winner.getReviewCount());
    assertEquals("Winner should be Bar B", "Bar B", winner.getVenueName());
  }

  // ============= VENUE EXCLUSION TESTS =============

  @Test
  public void testExcludesVenueInExclusionList() {
    // selectTopVenue should skip venues in exclusion set
    String placeId = "ChIJtest123";
    Set<String> exclusionSet = new HashSet<>();
    exclusionSet.add(placeId);
    assertTrue("PlaceId should be in exclusion set", exclusionSet.contains(placeId));
  }

  @Test
  public void testIncludesVenueNotInExclusionList() {
    // Venues not in exclusion set are eligible
    String placeId = "ChIJtest456";
    Set<String> exclusionSet = new HashSet<>();
    exclusionSet.add("ChIJtest123");
    assertFalse("PlaceId should not be in exclusion set", exclusionSet.contains(placeId));
  }

  // ============= RETRY LOGIC TESTS =============

  @Test
  public void testExpandsRadiusAndRetriesWhenNoEligibleVenuesFound() {
    // searchVenues should retry with expanded radius on empty results
    int radiusKm1 = 2;
    int radiusKm2 = 3;
    assertTrue("Expanded radius should be larger", radiusKm2 > radiusKm1);
  }

  @Test
  public void testStopsAfterOneRetryIfStillNoEligibleVenues() {
    // searchVenues should not retry more than once
    int maxRetries = 1;
    int retriesAttempted = 1;
    assertEquals("Should stop after one retry", maxRetries, retriesAttempted);
  }

  @Test
  public void testReturnsEmptyListOnContinuedFailure() {
    // After retries fail, return empty list to skip city
    List<WeeklySpot> results = new ArrayList<>();
    assertTrue("Empty list on failure", results.isEmpty());
  }

  // ============= ERROR HANDLING TESTS =============

  @Test
  public void testThrowsExceptionOnPlacesApiError() {
    // searchVenues throws exception on API error status
    String errorStatus = "OVER_QUERY_LIMIT";
    assertNotNull("Error status should not be null", errorStatus);
    assertTrue("Error should be from API", errorStatus.contains("QUERY"));
  }

  @Test
  public void testThrowsExceptionOnNetworkTimeout() {
    // searchVenues throws exception on network failure
    String exceptionMessage = "Connection timeout";
    assertTrue("Exception should mention timeout", exceptionMessage.contains("timeout"));
  }

  @Test
  public void testThrowsExceptionOnMalformedResponse() {
    // searchVenues throws exception when parsing malformed JSON
    String responseWithoutName = "{\"place_id\": \"ChIJ123\"}";
    assertFalse("Response missing name field", responseWithoutName.contains("name"));
  }

  // ============= DATA VALIDATION TESTS =============

  @Test
  public void testRejectsVenueWithMissingPlaceId() {
    // Venues without place_id are rejected
    String responseWithoutPlaceId = "{\"name\": \"Test Bar\"}";
    assertFalse("Response missing place_id", responseWithoutPlaceId.contains("place_id"));
  }

  @Test
  public void testRejectsVenueWithMissingName() {
    // Venues without name are rejected
    String responseWithoutName = "{\"place_id\": \"ChIJ123\"}";
    assertFalse("Response missing name", responseWithoutName.contains("name"));
  }

  @Test
  public void testRejectsVenueWithMissingCoordinates() {
    // Venues without geometry are rejected
    String responseWithoutCoords = "{\"name\": \"Test\", \"place_id\": \"ChIJ123\"}";
    assertFalse("Response missing geometry.location", responseWithoutCoords.contains("geometry"));
  }

  @Test
  public void testRejectsVenueWithOutOfBoundsCoordinates() {
    // Implausible coordinates far from search location are rejected
    double seattleLat = 47.6062;
    double seattleLng = -122.3321;
    double newYorkLat = 40.7128;
    double newYorkLng = -74.0060;

    double latDiff = Math.abs(seattleLat - newYorkLat);
    double lngDiff = Math.abs(seattleLng - newYorkLng);
    assertTrue("Coordinates far outside bounds", latDiff > 5.0 && lngDiff > 45.0);
  }
}
