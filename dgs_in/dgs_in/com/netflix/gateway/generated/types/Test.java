package com.netflix.gateway.generated.types;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class Test {
  private Votes id;

  public Test() {
  }

  public Test(Votes id) {
    this.id = id;
  }

  public Votes getId() {
    return id;
  }

  public void setId(Votes id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return "Test{" + "id='" + id + "'" +"}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Test that = (Test) o;
        return java.util.Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(id);
  }

  public static com.netflix.gateway.generated.types.Test.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private Votes id;

    public Test build() {
      com.netflix.gateway.generated.types.Test result = new com.netflix.gateway.generated.types.Test();
          result.id = this.id;
          return result;
    }

    public com.netflix.gateway.generated.types.Test.Builder id(Votes id) {
      this.id = id;
      return this;
    }
  }
}
