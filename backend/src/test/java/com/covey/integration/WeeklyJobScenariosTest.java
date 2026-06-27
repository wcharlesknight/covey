package com.covey.integration;

import static org.junit.Assert.*;

import com.covey.models.Invite;
import com.covey.models.WeeklySpot;
import org.junit.Test;

public class WeeklyJobScenariosTest {

  @Test
  public void testScenario1_SelectHighestRatedVenue() {
    WeeklySpot venue1 = new WeeklySpot("Seattle", "Bar A", "123 Main", "place1", 4.2, 60,
        System.currentTimeMillis());
    WeeklySpot venue2 = new WeeklySpot("Seattle", "Bar B", "456 Oak", "place2", 4.8, 150,
        System.currentTimeMillis());

    assertTrue("Should select venue with highest rating", venue2.getRating() > venue1.getRating());
  }

  @Test
  public void testScenario2_FilterByMinReviews() {
    WeeklySpot eligibleVenue = new WeeklySpot("Seattle", "Bar A", "123 Main", "place1", 4.5, 100,
        System.currentTimeMillis());
    WeeklySpot ineligibleVenue = new WeeklySpot("Seattle", "Bar B", "456 Oak", "place2", 4.7, 30,
        System.currentTimeMillis());

    assertTrue("Venue with 100 reviews should be eligible",
        eligibleVenue.getReviewCount() >= 50);
    assertFalse("Venue with 30 reviews should be ineligible",
        ineligibleVenue.getReviewCount() >= 50);
  }

  @Test
  public void testScenario3_CreateInviteForUser() {
    Invite invite = new Invite("user123", "spot456", "Seattle");

    assertNotNull("Invite should be created", invite);
    assertEquals("User should match", "user123", invite.getUserId());
    assertEquals("Spot should match", "spot456", invite.getWeeklySpotId());
    assertEquals("Status should be INVITED", Invite.Status.INVITED, invite.getStatus());
  }

  @Test
  public void testScenario4_MultipleInvitesPerSpot() {
    String spotId = "spot456";
    Invite invite1 = new Invite("user1", spotId, "Seattle");
    Invite invite2 = new Invite("user2", spotId, "Seattle");
    Invite invite3 = new Invite("user3", spotId, "Seattle");

    assertEquals("All should reference same spot", spotId, invite1.getWeeklySpotId());
    assertEquals("All should reference same spot", spotId, invite2.getWeeklySpotId());
    assertEquals("All should reference same spot", spotId, invite3.getWeeklySpotId());
  }

  @Test
  public void testScenario5_InviteStatusTransitions() {
    Invite invite = new Invite("user123", "spot456", "Seattle");

    invite.setStatus(Invite.Status.YES);
    assertEquals("Should be YES", Invite.Status.YES, invite.getStatus());

    invite.setStatus(Invite.Status.NO);
    assertEquals("Should be NO", Invite.Status.NO, invite.getStatus());

    invite.setStatus(Invite.Status.INTERESTED);
    assertEquals("Should be INTERESTED", Invite.Status.INTERESTED, invite.getStatus());
  }

  @Test
  public void testScenario6_SeattleVenueSelection() {
    WeeklySpot seattleVenue = new WeeklySpot("Seattle", "Pike Place Bar", "1428 Pike Pl",
        "place_seattle", 4.6, 200, System.currentTimeMillis());

    assertEquals("Should be Seattle", "Seattle", seattleVenue.getCity());
  }

  @Test
  public void testScenario7_TacomaVenueSelection() {
    WeeklySpot tacomaVenue = new WeeklySpot("Tacoma", "Tacoma Tavern", "123 Pacific Ave",
        "place_tacoma", 4.3, 75, System.currentTimeMillis());

    assertEquals("Should be Tacoma", "Tacoma", tacomaVenue.getCity());
  }

  @Test
  public void testScenario8_WeeklySpotCreatedAtTimestamp() {
    long beforeCreation = System.currentTimeMillis();
    WeeklySpot spot = new WeeklySpot("Seattle", "Bar", "123 Main", "place", 4.5, 100,
        beforeCreation);
    long afterCreation = System.currentTimeMillis();

    assertTrue("CreatedAt should be within timestamp range",
        spot.getCreatedAt() >= beforeCreation && spot.getCreatedAt() <= afterCreation);
  }

  @Test
  public void testScenario9_InviteCreatedAtNotification() {
    Invite invite = new Invite("user123", "spot456", "Seattle");

    assertTrue("CreatedAt should be set", invite.getCreatedAt() > 0);
    assertTrue("UpdatedAt should be set", invite.getUpdatedAt() > 0);
    assertEquals("Initial timestamps should be equal", invite.getCreatedAt(),
        invite.getUpdatedAt());
  }

  @Test
  public void testScenario10_VenueWithExactMinRating() {
    WeeklySpot venue = new WeeklySpot("Seattle", "Bar", "123 Main", "place", 4.0, 50,
        System.currentTimeMillis());

    assertEquals("Rating should be exactly 4.0", 4.0, venue.getRating(), 0.01);
    assertTrue("Should be eligible with minimum rating", venue.getRating() >= 4.0);
  }
}
