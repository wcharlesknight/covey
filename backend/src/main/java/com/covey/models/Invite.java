package com.covey.models;

public class Invite {
  public enum Status {
    INVITED, YES, NO, INTERESTED
  }

  private String id;
  private String userId;
  private String weekId;
  private String venueId;
  private String city;
  private Status status;
  private long createdAt;
  private long updatedAt;

  public Invite() {}

  public Invite(String userId, String weekId, String venueId, String city) {
    this.userId = userId;
    this.weekId = weekId;
    this.venueId = venueId;
    this.city = city;
    this.status = Status.INVITED;
    this.createdAt = System.currentTimeMillis();
    this.updatedAt = System.currentTimeMillis();
  }

  // Backward-compatible constructor for existing code (weeklySpotId → weekId)
  public Invite(String userId, String weeklySpotId, String city) {
    this.userId = userId;
    this.weekId = weeklySpotId;
    this.city = city;
    this.status = Status.INVITED;
    this.createdAt = System.currentTimeMillis();
    this.updatedAt = System.currentTimeMillis();
  }

  public static String generateId(String userId, String weekId) {
    return userId + "_" + weekId;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public String getWeekId() {
    return weekId;
  }

  public String getVenueId() {
    return venueId;
  }

  public String getCity() {
    return city;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
    this.updatedAt = System.currentTimeMillis();
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  // Backward-compatible getter for existing code
  public String getWeeklySpotId() {
    return weekId;
  }
}
