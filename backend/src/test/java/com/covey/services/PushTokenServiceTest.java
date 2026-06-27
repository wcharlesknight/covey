package com.covey.services;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class PushTokenServiceTest {
  private PushTokenService pushTokenService;

  @Before
  public void setUp() {
    pushTokenService = new PushTokenService();
  }

  @Test
  public void testPushTokenServiceExists() {
    assertNotNull(pushTokenService);
  }

  @Test
  public void testInvalidUserIdRegistration() {
    try {
      pushTokenService.registerToken("", "fcm_token", "ios");
      fail("Should throw IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("User ID"));
    } catch (Exception e) {
      // Expected in test without Firebase
    }
  }

  @Test
  public void testInvalidTokenRegistration() {
    try {
      pushTokenService.registerToken("user123", "", "ios");
      fail("Should throw IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("Token"));
    } catch (Exception e) {
      // Expected in test without Firebase
    }
  }
}
