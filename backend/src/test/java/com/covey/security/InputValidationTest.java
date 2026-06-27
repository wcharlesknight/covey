package com.covey.security;

import static org.junit.Assert.*;

import com.covey.models.User;
import org.junit.Test;

public class InputValidationTest {

  @Test
  public void testUserEmailValidation() {
    User user = new User("uid123", "valid@example.com", "John Doe", "Seattle");
    assertNotNull(user.getEmail());
    assertTrue(user.getEmail().contains("@"));
  }

  @Test
  public void testUserEmptyEmailValidation() {
    User user = new User("uid123", "", "John Doe", "Seattle");
    assertEquals("", user.getEmail());
  }

  @Test
  public void testUserNullEmailValidation() {
    User user = new User("uid123", null, "John Doe", "Seattle");
    assertNull(user.getEmail());
  }

  @Test
  public void testUserCityValidation() {
    User user = new User("uid123", "user@example.com", "John Doe", "Seattle");
    assertEquals("Seattle", user.getCity());
  }

  @Test
  public void testUserInvalidCityValidation() {
    User user = new User("uid123", "user@example.com", "John Doe", "InvalidCity");
    assertEquals("InvalidCity", user.getCity());
  }

  @Test
  public void testUserDisplayNameValidation() {
    User user = new User("uid123", "user@example.com", "John Doe", "Seattle");
    assertNotNull(user.getDisplayName());
    assertTrue(user.getDisplayName().length() > 0);
  }

  @Test
  public void testUserUidLength() {
    User user = new User("a", "user@example.com", "John Doe", "Seattle");
    assertEquals("a", user.getUid());

    User user2 = new User("very_long_uid_value_that_is_valid", "user@example.com", "Jane", "Seattle");
    assertEquals("very_long_uid_value_that_is_valid", user2.getUid());
  }
}
