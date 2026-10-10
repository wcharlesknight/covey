package com.covey.services;

import static org.junit.Assert.*;

import com.covey.models.WeeklySpot;
import org.junit.Before;
import org.junit.Test;

public class EmailServiceTest {
  private EmailService emailService;
  private WeeklySpot spot;

  @Before
  public void setUp() {
    emailService = new EmailService(null);
    spot = new WeeklySpot("Seattle", "2026-W41", "The Nest", "123 Main St, Seattle WA",
        "venue_abc", 4.5, 212, 1728518400L);
  }

  @Test
  public void htmlBody_hasCoveyBranding() {
    String html = emailService.buildHtmlBody(spot, "Alex");
    assertTrue("Should contain Covey wordmark", html.contains(">Covey<"));
  }

  @Test
  public void htmlBody_doesNotHaveOldH2Header() {
    String html = emailService.buildHtmlBody(spot, "Alex");
    assertFalse("Should not contain old h2 header", html.contains("<h2"));
  }

  @Test
  public void htmlBody_containsVenueDetails() {
    String html = emailService.buildHtmlBody(spot, "Alex");
    assertTrue(html.contains("The Nest"));
    assertTrue(html.contains("Seattle"));
    assertTrue(html.contains("Alex"));
  }

  @Test
  public void textBody_containsCoveyTeam() {
    String text = emailService.buildTextBody(spot, "Alex");
    assertTrue(text.contains("Covey"));
    assertTrue(text.contains("Alex"));
    assertTrue(text.contains("The Nest"));
  }

  @Test
  public void htmlBody_withLogoUrl_includesImgTag() {
    String url = "https://storage.googleapis.com/covey/icon.png";
    String html = emailService.buildHtmlBodyWithLogo(spot, "Alex", url);
    assertTrue("Should have img tag when logo URL provided", html.contains("<img src='" + url + "'"));
  }

  @Test
  public void htmlBody_withoutLogoUrl_noImgTag() {
    String html = emailService.buildHtmlBodyWithLogo(spot, "Alex", "");
    assertFalse("Should not have img tag when no logo URL", html.contains("<img"));
    assertTrue("Should still have Covey wordmark", html.contains(">Covey<"));
  }
}
