package com.covey.models;

public class PushToken {
  private String id;
  private String userId;
  private String token;
  private String platform;
  private boolean isActive;
  private long createdAt;
  private long lastUsedAt;

  public PushToken() {}

  public PushToken(String userId, String token, String platform) {
    this.userId = userId;
    this.token = token;
    this.platform = platform;
    this.isActive = true;
    this.createdAt = System.currentTimeMillis();
    this.lastUsedAt = System.currentTimeMillis();
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

  public String getToken() {
    return token;
  }

  public String getPlatform() {
    return platform;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean active) {
    isActive = active;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public long getLastUsedAt() {
    return lastUsedAt;
  }

  public void setLastUsedAt(long lastUsedAt) {
    this.lastUsedAt = lastUsedAt;
  }
}
