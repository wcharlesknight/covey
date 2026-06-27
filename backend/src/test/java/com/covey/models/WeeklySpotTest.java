package com.covey.models;

import static org.junit.Assert.*;

import org.junit.Test;

public class WeeklySpotTest {

  @Test
  public void testWeeklySpotCreation() {
    long weekStart = System.currentTimeMillis();
    WeeklySpot spot = new WeeklySpot("Seattle", "The Tavern", "123 Main St", "place123",
        4.5, 150, weekStart);

    assertNotNull(spot);
    assertEquals("Seattle", spot.getCity());
    assertEquals("The Tavern", spot.getVenueName());
    assertEquals("123 Main St", spot.getVenueAddress());
    assertEquals("place123", spot.getVenueId());
    assertEquals(4.5, spot.getRating(), 0.1);
    assertEquals(150, spot.getReviewCount());
    assertEquals(weekStart, spot.getWeekStartDate());
    assertTrue(spot.getCreatedAt() > 0);
  }

  @Test
  public void testWeeklySpotIdAssignment() {
    WeeklySpot spot = new WeeklySpot("Seattle", "The Tavern", "123 Main St", "place123",
        4.5, 150, System.currentTimeMillis());
    assertNull(spot.getId());

    spot.setId("week-2026-27");
    assertEquals("week-2026-27", spot.getId());
  }
}
