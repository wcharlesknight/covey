package com.covey.models;

public class WeeklySpot {
  private String id;
  private String city;
  private String venueName;
  private String venueAddress;
  private String venueId;
  private double rating;
  private int reviewCount;
  private long weekStartDate;
  private long createdAt;

  public WeeklySpot() {}

  public WeeklySpot(String city, String venueName, String venueAddress, String venueId,
      double rating, int reviewCount, long weekStartDate) {
    this.city = city;
    this.venueName = venueName;
    this.venueAddress = venueAddress;
    this.venueId = venueId;
    this.rating = rating;
    this.reviewCount = reviewCount;
    this.weekStartDate = weekStartDate;
    this.createdAt = System.currentTimeMillis();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getCity() {
    return city;
  }

  public String getVenueName() {
    return venueName;
  }

  public String getVenueAddress() {
    return venueAddress;
  }

  public String getVenueId() {
    return venueId;
  }

  public double getRating() {
    return rating;
  }

  public int getReviewCount() {
    return reviewCount;
  }

  public long getWeekStartDate() {
    return weekStartDate;
  }

  public long getCreatedAt() {
    return createdAt;
  }
}
