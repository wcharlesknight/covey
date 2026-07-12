package com.covey.models;

import java.util.ArrayList;
import java.util.List;

public class WeeklyJobRun {
  public static class CityResult {
    public String city;
    public String venueSelected;
    public int inviteCount;
    public int pushCount;
    public int emailCount;
    public List<String> errors;

    public CityResult(String city) {
      this.city = city;
      this.venueSelected = null;
      this.inviteCount = 0;
      this.pushCount = 0;
      this.emailCount = 0;
      this.errors = new ArrayList<>();
    }
  }

  private String id;
  private String weekId;
  private long startedAt;
  private long completedAt;
  private int citiesProcessed;
  private int citiesSkipped;
  private int totalInvites;
  private int totalPushes;
  private int totalEmails;
  private List<CityResult> cityResults;

  public WeeklyJobRun(String jobId, String weekId) {
    this.id = jobId;
    this.weekId = weekId;
    this.startedAt = System.currentTimeMillis();
    this.cityResults = new ArrayList<>();
  }

  public String getId() {
    return id;
  }

  public String getWeekId() {
    return weekId;
  }

  public long getStartedAt() {
    return startedAt;
  }

  public long getCompletedAt() {
    return completedAt;
  }

  public void markCompleted() {
    this.completedAt = System.currentTimeMillis();
  }

  public int getCitiesProcessed() {
    return citiesProcessed;
  }

  public void setCitiesProcessed(int count) {
    this.citiesProcessed = count;
  }

  public int getCitiesSkipped() {
    return citiesSkipped;
  }

  public void setCitiesSkipped(int count) {
    this.citiesSkipped = count;
  }

  public int getTotalInvites() {
    return totalInvites;
  }

  public void setTotalInvites(int count) {
    this.totalInvites = count;
  }

  public int getTotalPushes() {
    return totalPushes;
  }

  public void setTotalPushes(int count) {
    this.totalPushes = count;
  }

  public int getTotalEmails() {
    return totalEmails;
  }

  public void setTotalEmails(int count) {
    this.totalEmails = count;
  }

  public List<CityResult> getCityResults() {
    return cityResults;
  }

  public void addCityResult(CityResult result) {
    cityResults.add(result);
  }
}
