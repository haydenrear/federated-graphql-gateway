package com.netflix.gateway.generated.types;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class TickEdge {
  private String cursor;

  private Tick node;

  public TickEdge() {
  }

  public TickEdge(String cursor, Tick node) {
    this.cursor = cursor;
    this.node = node;
  }

  public String getCursor() {
    return cursor;
  }

  public void setCursor(String cursor) {
    this.cursor = cursor;
  }

  public Tick getNode() {
    return node;
  }

  public void setNode(Tick node) {
    this.node = node;
  }

  @Override
  public String toString() {
    return "TickEdge{" + "cursor='" + cursor + "'," +"node='" + node + "'" +"}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TickEdge that = (TickEdge) o;
        return java.util.Objects.equals(cursor, that.cursor) &&
                            java.util.Objects.equals(node, that.node);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(cursor, node);
  }

  public static com.netflix.gateway.generated.types.TickEdge.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private String cursor;

    private Tick node;

    public TickEdge build() {
                  com.netflix.gateway.generated.types.TickEdge result = new com.netflix.gateway.generated.types.TickEdge();
                      result.cursor = this.cursor;
          result.node = this.node;
                      return result;
    }

    public com.netflix.gateway.generated.types.TickEdge.Builder cursor(String cursor) {
      this.cursor = cursor;
      return this;
    }

    public com.netflix.gateway.generated.types.TickEdge.Builder node(Tick node) {
      this.node = node;
      return this;
    }
  }
}
