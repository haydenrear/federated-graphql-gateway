package com.netflix.gateway.generated.types;

import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;

public class TicksConnection {
  private List<TickEdge> edges;

  public TicksConnection() {
  }

  public TicksConnection(List<TickEdge> edges) {
    this.edges = edges;
  }

  public List<TickEdge> getEdges() {
    return edges;
  }

  public void setEdges(List<TickEdge> edges) {
    this.edges = edges;
  }

  @Override
  public String toString() {
    return "TicksConnection{" + "edges='" + edges + "'" +"}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TicksConnection that = (TicksConnection) o;
        return java.util.Objects.equals(edges, that.edges);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(edges);
  }

  public static com.netflix.gateway.generated.types.TicksConnection.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private List<TickEdge> edges;

    public TicksConnection build() {
      com.netflix.gateway.generated.types.TicksConnection result = new com.netflix.gateway.generated.types.TicksConnection();
          result.edges = this.edges;
          return result;
    }

    public com.netflix.gateway.generated.types.TicksConnection.Builder edges(List<TickEdge> edges) {
      this.edges = edges;
      return this;
    }
  }
}
