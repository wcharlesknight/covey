package com.covey.models;

public class User {
  private String uid;
  private String email;
  private String displayName;
  private String photoURL;
  private String city;
  private String createdAt;
  private String updatedAt;

  public User() {}

  public User(String uid, String email, String displayName, String city) {
    this.uid = uid;
    this.email = email;
    this.displayName = displayName;
    this.city = city;
    this.createdAt = new java.util.Date().toInstant().toString();
    this.updatedAt = new java.util.Date().toInstant().toString();
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
    this.updatedAt = new java.util.Date().toInstant().toString();
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
    this.updatedAt = new java.util.Date().toInstant().toString();
  }

  public String getPhotoURL() {
    return photoURL;
  }

  public void setPhotoURL(String photoURL) {
    this.photoURL = photoURL;
    this.updatedAt = new java.util.Date().toInstant().toString();
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
    this.updatedAt = new java.util.Date().toInstant().toString();
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public String getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
  }
}
