package com.covey.models;

import static org.junit.Assert.*;

import org.junit.Test;

public class InviteTest {

  @Test
  public void testInviteCreation() {
    Invite invite = new Invite("user123", "spot456", "Seattle");

    assertNotNull(invite);
    assertEquals("user123", invite.getUserId());
    assertEquals("spot456", invite.getWeeklySpotId());
    assertEquals("Seattle", invite.getCity());
    assertEquals(Invite.Status.INVITED, invite.getStatus());
    assertTrue(invite.getCreatedAt() > 0);
  }

  @Test
  public void testInviteStatusUpdate() {
    Invite invite = new Invite("user123", "spot456", "Seattle");
    long originalCreatedAt = invite.getCreatedAt();

    invite.setStatus(Invite.Status.YES);

    assertEquals(Invite.Status.YES, invite.getStatus());
    assertEquals(originalCreatedAt, invite.getCreatedAt());
    assertTrue(invite.getUpdatedAt() >= originalCreatedAt);
  }

  @Test
  public void testAllInviteStatuses() {
    Invite invite = new Invite("user123", "spot456", "Seattle");

    invite.setStatus(Invite.Status.YES);
    assertEquals(Invite.Status.YES, invite.getStatus());

    invite.setStatus(Invite.Status.NO);
    assertEquals(Invite.Status.NO, invite.getStatus());

    invite.setStatus(Invite.Status.INTERESTED);
    assertEquals(Invite.Status.INTERESTED, invite.getStatus());

    invite.setStatus(Invite.Status.INVITED);
    assertEquals(Invite.Status.INVITED, invite.getStatus());
  }
}
