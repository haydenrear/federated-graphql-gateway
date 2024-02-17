package com.netflix.gateway.generated.types;

import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class Tick {
  private String id;

  private Integer userStars;

  private String userRating;

  private String comments;

  public Tick() {
  }

  public Tick(String id, Integer userStars, String userRating, String comments) {
    this.id = id;
    this.userStars = userStars;
    this.userRating = userRating;
    this.comments = comments;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Integer getUserStars() {
    return userStars;
  }

  public void setUserStars(Integer userStars) {
    this.userStars = userStars;
  }

  public String getUserRating() {
    return userRating;
  }

  public void setUserRating(String userRating) {
    this.userRating = userRating;
  }

  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }

  @Override
  public String toString() {
    return "Tick{" + "id='" + id + "'," +"userStars='" + userStars + "'," +"userRating='" + userRating + "'," +"comments='" + comments + "'" +"}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tick that = (Tick) o;
        return java.util.Objects.equals(id, that.id) &&
                            java.util.Objects.equals(userStars, that.userStars) &&
                            java.util.Objects.equals(userRating, that.userRating) &&
                            java.util.Objects.equals(comments, that.comments);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(id, userStars, userRating, comments);
  }

  public static com.netflix.gateway.generated.types.Tick.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private String id;

    private Integer userStars;

    private String userRating;

    private String comments;

    public Tick build() {
                  com.netflix.gateway.generated.types.Tick result = new com.netflix.gateway.generated.types.Tick();
                      result.id = this.id;
          result.userStars = this.userStars;
          result.userRating = this.userRating;
          result.comments = this.comments;
                      return result;
    }

    public com.netflix.gateway.generated.types.Tick.Builder id(String id) {
      this.id = id;
      return this;
    }

    public com.netflix.gateway.generated.types.Tick.Builder userStars(Integer userStars) {
      this.userStars = userStars;
      return this;
    }

    public com.netflix.gateway.generated.types.Tick.Builder userRating(String userRating) {
      this.userRating = userRating;
      return this;
    }

    public com.netflix.gateway.generated.types.Tick.Builder comments(String comments) {
      this.comments = comments;
      return this;
    }
  }
}
