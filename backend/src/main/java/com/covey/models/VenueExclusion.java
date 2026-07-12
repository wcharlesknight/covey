package com.covey.models;

import java.util.ArrayList;
import java.util.List;

public class VenueExclusion {
  private String id;
  private String city;
  private String weekId;
  private List<String> venueIds;
  private long updatedAt;

  public VenueExclusion() {
    this.venueIds = new ArrayList<>();
  }

  public VenueExclusion(String city, String weekId, String venueId) {
    this.city = city;
    this.weekId = weekId;
    this.venueIds = new ArrayList<>();
    this.venueIds.add(venueId);
    this.updatedAt = System.currentTimeMillis();
  }

  public static String generateId(String city, String weekId) {
    return city + "_" + weekId;
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

  public void setCity(String city) {
    this.city = city;
  }

  public void setWeekId(String weekId) {
    this.weekId = weekId;
  }

  public String getWeekId() {
    return weekId;
  }

  public List<String> getVenueIds() {
    return venueIds;
  }

  public void setVenueIds(List<String> venueIds) {
    this.venueIds = venueIds;
  }

  public void setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }
}
