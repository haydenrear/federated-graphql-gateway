package com.netflix.gateway.generated.types;

import java.lang.Double;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class Votes {
  private Double starRating;

  private Integer nrOfVotes;

  public Votes() {
  }

  public Votes(Double starRating, Integer nrOfVotes) {
    this.starRating = starRating;
    this.nrOfVotes = nrOfVotes;
  }

  public Double getStarRating() {
    return starRating;
  }

  public void setStarRating(Double starRating) {
    this.starRating = starRating;
  }

  public Integer getNrOfVotes() {
    return nrOfVotes;
  }

  public void setNrOfVotes(Integer nrOfVotes) {
    this.nrOfVotes = nrOfVotes;
  }

  @Override
  public String toString() {
    return "Votes{" + "starRating='" + starRating + "'," +"nrOfVotes='" + nrOfVotes + "'" +"}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Votes that = (Votes) o;
        return java.util.Objects.equals(starRating, that.starRating) &&
                            java.util.Objects.equals(nrOfVotes, that.nrOfVotes);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(starRating, nrOfVotes);
  }

  public static com.netflix.gateway.generated.types.Votes.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private Double starRating;

    private Integer nrOfVotes;

    public Votes build() {
                  com.netflix.gateway.generated.types.Votes result = new com.netflix.gateway.generated.types.Votes();
                      result.starRating = this.starRating;
          result.nrOfVotes = this.nrOfVotes;
                      return result;
    }

    public com.netflix.gateway.generated.types.Votes.Builder starRating(Double starRating) {
      this.starRating = starRating;
      return this;
    }

    public com.netflix.gateway.generated.types.Votes.Builder nrOfVotes(Integer nrOfVotes) {
      this.nrOfVotes = nrOfVotes;
      return this;
    }
  }
}
