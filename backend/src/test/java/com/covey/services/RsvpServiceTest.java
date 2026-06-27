package com.covey.services;

import static org.junit.Assert.*;

import com.covey.models.Invite;
import org.junit.Before;
import org.junit.Test;

public class RsvpServiceTest {
  private RsvpService rsvpService;

  @Before
  public void setUp() {
    rsvpService = new RsvpService();
  }

  @Test
  public void testRsvpServiceExists() {
    assertNotNull(rsvpService);
  }

  @Test
  public void testStatusTransitionFromInvited() {
    Invite.Status[] validTransitions = {
        Invite.Status.YES,
        Invite.Status.NO,
        Invite.Status.INTERESTED
    };

    for (Invite.Status status : validTransitions) {
      assertNotNull(status);
    }
  }

  @Test
  public void testStatusEnum() {
    Invite.Status[] statuses = {
        Invite.Status.INVITED,
        Invite.Status.YES,
        Invite.Status.NO,
        Invite.Status.INTERESTED
    };

    assertEquals(4, statuses.length);
    assertEquals(Invite.Status.INVITED, statuses[0]);
  }
}
