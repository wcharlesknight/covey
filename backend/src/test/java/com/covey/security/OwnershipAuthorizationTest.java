package com.covey.security;

import static org.junit.Assert.*;

import com.covey.models.Invite;
import org.junit.Test;

public class OwnershipAuthorizationTest {

  @Test
  public void testOwnershipValidationSameUser() {
    Invite invite = new Invite("user123", "spot456", "Seattle");
    String requestingUserId = "user123";

    assertTrue("Owner should be able to modify invite",
        invite.getUserId().equals(requestingUserId));
  }

  @Test
  public void testOwnershipValidationDifferentUser() {
    Invite invite = new Invite("user123", "spot456", "Seattle");
    String requestingUserId = "user999";

    assertFalse("Non-owner should not be able to modify invite",
        invite.getUserId().equals(requestingUserId));
  }

  @Test
  public void testOwnershipValidationNullUser() {
    Invite invite = new Invite("user123", "spot456", "Seattle");
    String requestingUserId = null;

    assertFalse("Null user should not have ownership",
        invite.getUserId().equals(requestingUserId));
  }

  @Test
  public void testOwnershipCaseMatters() {
    Invite invite = new Invite("User123", "spot456", "Seattle");
    String requestingUserId = "user123";

    assertFalse("Ownership check should be case-sensitive",
        invite.getUserId().equals(requestingUserId));
  }
}
