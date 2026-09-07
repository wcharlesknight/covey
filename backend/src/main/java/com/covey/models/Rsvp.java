package com.covey.models;

public class Rsvp {
  public enum Status {
    YES, NO, INTERESTED
  }

  private String id;
  private String userId;
  private String spotId;
  private String city;
  private String weekId;
  private Status status;
  private long createdAt;
  private long updatedAt;

  public Rsvp() {}

  public Rsvp(String userId, String spotId, String city, String weekId, Status status) {
    this.id = generateId(spotId, userId);
    this.userId = userId;
    this.spotId = spotId;
    this.city = city;
    this.weekId = weekId;
    this.status = status;
    this.createdAt = System.currentTimeMillis();
    this.updatedAt = this.createdAt;
  }

  public static String generateId(String spotId, String userId) {
    return spotId + "_" + userId;
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

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getSpotId() {
    return spotId;
  }

  public void setSpotId(String spotId) {
    this.spotId = spotId;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getWeekId() {
    return weekId;
  }

  public void setWeekId(String weekId) {
    this.weekId = weekId;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
  }
}
