package com.covey.models;

import static org.junit.Assert.*;

import org.junit.Test;

public class PushTokenTest {

  @Test
  public void testPushTokenCreation() {
    PushToken token = new PushToken("user123", "fcm_token_abc123", "ios");

    assertNotNull(token);
    assertEquals("user123", token.getUserId());
    assertEquals("fcm_token_abc123", token.getToken());
    assertEquals("ios", token.getPlatform());
    assertTrue(token.isActive());
    assertTrue(token.getCreatedAt() > 0);
  }

  @Test
  public void testPushTokenInactivation() {
    PushToken token = new PushToken("user123", "fcm_token_abc123", "ios");
    assertTrue(token.isActive());

    token.setActive(false);
    assertFalse(token.isActive());
  }

  @Test
  public void testPushTokenLastUsed() {
    PushToken token = new PushToken("user123", "fcm_token_abc123", "ios");
    long originalLastUsed = token.getLastUsedAt();

    long newTime = System.currentTimeMillis() + 1000;
    token.setLastUsedAt(newTime);

    assertEquals(newTime, token.getLastUsedAt());
    assertTrue(token.getLastUsedAt() > originalLastUsed);
  }
}
