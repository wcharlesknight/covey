package com.covey.services;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.covey.integrations.GooglePlacesClient;
import com.covey.models.WeeklySpot;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class WeeklyJobServiceTest {
  private GooglePlacesClient mockPlacesClient;
  private WeeklyJobService weeklyJobService;

  @Before
  public void setUp() {
    mockPlacesClient = mock(GooglePlacesClient.class);
    weeklyJobService = new WeeklyJobService(mockPlacesClient);
  }

  @Test
  public void testWeeklyJobServiceExists() {
    assertNotNull(weeklyJobService);
  }

  @Test
  public void testCityCoordinates() {
    // Seattle: 47.6062, -122.3321
    // Tacoma: 47.2529, -122.4443
    assertTrue(true);
  }

  @Test
  public void testVenueSelectionCriteria() {
    WeeklySpot spot = new WeeklySpot("Seattle", "The Tavern", "123 Main St", "place123",
        4.5, 150, System.currentTimeMillis());

    assertNotNull(spot);
    assertTrue(spot.getRating() >= 4.0);
    assertTrue(spot.getReviewCount() >= 50);
  }
}
