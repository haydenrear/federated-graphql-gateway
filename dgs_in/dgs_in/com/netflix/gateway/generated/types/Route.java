package com.netflix.gateway.generated.types;

import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;

public class Route {
  private String routeId;

  private String name;

  private String grade;

  private Integer pitches;

  private Votes votes;

  private List<String> location;

  public Route() {
  }

  public Route(String routeId, String name, String grade, Integer pitches, Votes votes,
      List<String> location) {
    this.routeId = routeId;
    this.name = name;
    this.grade = grade;
    this.pitches = pitches;
    this.votes = votes;
    this.location = location;
  }

  public String getRouteId() {
    return routeId;
  }

  public void setRouteId(String routeId) {
    this.routeId = routeId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getGrade() {
    return grade;
  }

  public void setGrade(String grade) {
    this.grade = grade;
  }

  public Integer getPitches() {
    return pitches;
  }

  public void setPitches(Integer pitches) {
    this.pitches = pitches;
  }

  public Votes getVotes() {
    return votes;
  }

  public void setVotes(Votes votes) {
    this.votes = votes;
  }

  public List<String> getLocation() {
    return location;
  }

  public void setLocation(List<String> location) {
    this.location = location;
  }

  @Override
  public String toString() {
    return "Route{" + "routeId='" + routeId + "'," +"name='" + name + "'," +"grade='" + grade + "'," +"pitches='" + pitches + "'," +"votes='" + votes + "'," +"location='" + location + "'" +"}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Route that = (Route) o;
        return java.util.Objects.equals(routeId, that.routeId) &&
                            java.util.Objects.equals(name, that.name) &&
                            java.util.Objects.equals(grade, that.grade) &&
                            java.util.Objects.equals(pitches, that.pitches) &&
                            java.util.Objects.equals(votes, that.votes) &&
                            java.util.Objects.equals(location, that.location);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(routeId, name, grade, pitches, votes, location);
  }

  public static com.netflix.gateway.generated.types.Route.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private String routeId;

    private String name;

    private String grade;

    private Integer pitches;

    private Votes votes;

    private List<String> location;

    public Route build() {
                  com.netflix.gateway.generated.types.Route result = new com.netflix.gateway.generated.types.Route();
                      result.routeId = this.routeId;
          result.name = this.name;
          result.grade = this.grade;
          result.pitches = this.pitches;
          result.votes = this.votes;
          result.location = this.location;
                      return result;
    }

    public com.netflix.gateway.generated.types.Route.Builder routeId(String routeId) {
      this.routeId = routeId;
      return this;
    }

    public com.netflix.gateway.generated.types.Route.Builder name(String name) {
      this.name = name;
      return this;
    }

    public com.netflix.gateway.generated.types.Route.Builder grade(String grade) {
      this.grade = grade;
      return this;
    }

    public com.netflix.gateway.generated.types.Route.Builder pitches(Integer pitches) {
      this.pitches = pitches;
      return this;
    }

    public com.netflix.gateway.generated.types.Route.Builder votes(Votes votes) {
      this.votes = votes;
      return this;
    }

    public com.netflix.gateway.generated.types.Route.Builder location(List<String> location) {
      this.location = location;
      return this;
    }
  }
}
