package com.covey.integration;

import static org.junit.Assert.*;

import com.covey.models.Invite;
import com.covey.models.PushToken;
import com.covey.models.User;
import com.covey.models.WeeklySpot;
import org.junit.Test;

public class DataIntegrityTest {

  @Test
  public void testUserCreatedAtImmutable() {
    User user = new User("uid123", "user@example.com", "John", "Seattle");
    String originalCreatedAt = user.getCreatedAt();

    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      fail();
    }

    user.setEmail("newemail@example.com");

    assertEquals("Created timestamp should not change", originalCreatedAt, user.getCreatedAt());
  }

  @Test
  public void testInviteStatusUpdateTimestamp() {
    Invite invite = new Invite("uid123", "spot456", "Seattle");
    long originalUpdatedAt = invite.getUpdatedAt();

    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      fail();
    }

    invite.setStatus(Invite.Status.YES);

    assertTrue("Updated timestamp should increase", invite.getUpdatedAt() > originalUpdatedAt);
  }

  @Test
  public void testWeeklySpotIdAssignment() {
    WeeklySpot spot = new WeeklySpot("Seattle", "Bar", "123 Main", "place123", 4.5, 100,
        System.currentTimeMillis());
    assertNull("ID should be null initially", spot.getId());

    String newId = "week-2026-27";
    spot.setId(newId);
    assertEquals("ID should be settable", newId, spot.getId());
  }

  @Test
  public void testPushTokenPlatformTracking() {
    PushToken iosToken = new PushToken("uid123", "ios_fcm_token", "ios");
    assertEquals("ios", iosToken.getPlatform());

    PushToken androidToken = new PushToken("uid123", "android_fcm_token", "android");
    assertEquals("android", androidToken.getPlatform());
  }

  @Test
  public void testPushTokenActiveStatus() {
    PushToken token = new PushToken("uid123", "fcm_token", "ios");
    assertTrue("Token should be active initially", token.isActive());

    token.setActive(false);
    assertFalse("Token should be deactivatable", token.isActive());

    token.setActive(true);
    assertTrue("Token should be reactivatable", token.isActive());
  }
}
