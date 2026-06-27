package com.covey.models;

public class User {
  private String uid;
  private String email;
  private String displayName;
  private String city;
  private long createdAt;
  private long updatedAt;

  public User() {}

  public User(String uid, String email, String displayName, String city) {
    this.uid = uid;
    this.email = email;
    this.displayName = displayName;
    this.city = city;
    this.createdAt = System.currentTimeMillis();
    this.updatedAt = System.currentTimeMillis();
  }

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
    this.updatedAt = System.currentTimeMillis();
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
    this.updatedAt = System.currentTimeMillis();
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
    this.updatedAt = System.currentTimeMillis();
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
  }
}
