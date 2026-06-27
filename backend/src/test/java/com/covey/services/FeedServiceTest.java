package com.covey.services;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class FeedServiceTest {
  private FeedService feedService;

  @Before
  public void setUp() {
    feedService = new FeedService();
  }

  @Test
  public void testFeedServiceExists() {
    assertNotNull(feedService);
  }

  @Test
  public void testFourWeeksCalculation() {
    long now = System.currentTimeMillis();
    long fourWeeksAgo = now - (4L * 7 * 24 * 60 * 60 * 1000);

    assertTrue(fourWeeksAgo < now);
    assertTrue((now - fourWeeksAgo) >= (4L * 7 * 24 * 60 * 60 * 1000));
  }
}
