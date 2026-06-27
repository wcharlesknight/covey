package com.covey.models;

public class Invite {
  public enum Status {
    INVITED, YES, NO, INTERESTED
  }

  private String id;
  private String userId;
  private String weeklySpotId;
  private String city;
  private Status status;
  private long createdAt;
  private long updatedAt;

  public Invite() {}

  public Invite(String userId, String weeklySpotId, String city) {
    this.userId = userId;
    this.weeklySpotId = weeklySpotId;
    this.city = city;
    this.status = Status.INVITED;
    this.createdAt = System.currentTimeMillis();
    this.updatedAt = System.currentTimeMillis();
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

  public String getWeeklySpotId() {
    return weeklySpotId;
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
}
