package com.covey.models;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RsvpTest {

  @Test
  public void testGenerateIdCombinesSpotAndUser() {
    assertEquals("Seattle_2026-W36_user123", Rsvp.generateId("Seattle_2026-W36", "user123"));
  }

  @Test
  public void testConstructorSetsFields() {
    Rsvp rsvp = new Rsvp("user123", "Seattle_2026-W36", "Seattle", "2026-W36", Rsvp.Status.YES);

    assertEquals("Seattle_2026-W36_user123", rsvp.getId());
    assertEquals("user123", rsvp.getUserId());
    assertEquals("Seattle_2026-W36", rsvp.getSpotId());
    assertEquals("Seattle", rsvp.getCity());
    assertEquals("2026-W36", rsvp.getWeekId());
    assertEquals(Rsvp.Status.YES, rsvp.getStatus());
    assertEquals(rsvp.getCreatedAt(), rsvp.getUpdatedAt());
  }

  @Test
  public void testStatusEnumValues() {
    Rsvp.Status[] statuses = Rsvp.Status.values();
    assertEquals(3, statuses.length);
    assertEquals(Rsvp.Status.YES, Rsvp.Status.valueOf("YES"));
    assertEquals(Rsvp.Status.NO, Rsvp.Status.valueOf("NO"));
    assertEquals(Rsvp.Status.INTERESTED, Rsvp.Status.valueOf("INTERESTED"));
  }
}
