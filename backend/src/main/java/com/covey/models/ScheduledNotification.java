package com.covey.models;

public class ScheduledNotification {
  public enum Channel {
    FCM, EMAIL
  }

  public enum Status {
    PENDING, SENT, FAILED
  }

  private String id;
  private String userId;
  private String weekId;
  private String city;
  private Channel channel;
  private Status status;
  private long deliverAt;
  private long createdAt;
  private String errorMessage;

  public ScheduledNotification() {}

  public ScheduledNotification(String userId, String weekId, String city, Channel channel, long deliverAtTimestamp) {
    this.userId = userId;
    this.weekId = weekId;
    this.city = city;
    this.channel = channel;
    this.status = Status.PENDING;
    this.deliverAt = deliverAtTimestamp;
    this.createdAt = System.currentTimeMillis();
  }

  public static String generateId(String userId, String weekId, Channel channel) {
    return userId + "_" + weekId + "_" + channel.name();
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

  public String getCity() {
    return city;
  }

  public Channel getChannel() {
    return channel;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public long getDeliverAt() {
    return deliverAt;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
