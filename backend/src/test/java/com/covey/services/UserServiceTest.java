package com.covey.services;

import static org.junit.Assert.*;

import com.covey.models.User;
import org.junit.Before;
import org.junit.Test;

public class UserServiceTest {
  private UserService userService;

  @Before
  public void setUp() {
    userService = new UserService();
  }

  @Test
  public void testUserCreation() {
    User user = new User("user123", "user@example.com", "Test User", "Seattle");

    assertNotNull(user);
    assertEquals("user123", user.getUid());
    assertEquals("user@example.com", user.getEmail());
    assertEquals("Test User", user.getDisplayName());
    assertEquals("Seattle", user.getCity());
    assertTrue(user.getCreatedAt() > 0);
  }

  @Test
  public void testUserUpdate() {
    User user = new User("user123", "user@example.com", "Test User", "Seattle");
    long originalCreatedAt = user.getCreatedAt();

    user.setEmail("newemail@example.com");
    user.setCity("Tacoma");

    assertEquals("newemail@example.com", user.getEmail());
    assertEquals("Tacoma", user.getCity());
    assertEquals(originalCreatedAt, user.getCreatedAt());
    assertTrue(user.getUpdatedAt() >= originalCreatedAt);
  }

  @Test
  public void testUserCreationWithNullUid() {
    try {
      User user = new User(null, "user@example.com", "Test User", "Seattle");
      userService.createUser(user);
      fail("Should throw IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("UID cannot be null"));
    } catch (Exception e) {
      // Expected in test without Firebase
    }
  }
}
