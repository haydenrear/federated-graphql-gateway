package com.netflix.gateway.generated.client;

import com.netflix.graphql.dgs.client.codegen.GraphQLQuery;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.HashSet;
import java.util.Set;

public class TicksGraphQLQuery extends GraphQLQuery {
  public TicksGraphQLQuery(Integer first, Integer after, Boolean allowCached, String queryName,
      Set<String> fieldsSet) {
    super("query", queryName);
    if (first != null || fieldsSet.contains("first")) {
        getInput().put("first", first);
    }if (after != null || fieldsSet.contains("after")) {
        getInput().put("after", after);
    }if (allowCached != null || fieldsSet.contains("allowCached")) {
        getInput().put("allowCached", allowCached);
    }
  }

  public TicksGraphQLQuery() {
    super("query");
  }

  @Override
  public String getOperationName() {
     return "ticks";
                    
  }

  public static Builder newRequest() {
    return new Builder();
  }

  public static class Builder {
    private Set<String> fieldsSet = new HashSet<>();

    private Integer first;

    private Integer after;

    private Boolean allowCached;

    private String queryName;

    public TicksGraphQLQuery build() {
      return new TicksGraphQLQuery(first, after, allowCached, queryName, fieldsSet);
               
    }

    public Builder first(Integer first) {
      this.first = first;
      this.fieldsSet.add("first");
      return this;
    }

    public Builder after(Integer after) {
      this.after = after;
      this.fieldsSet.add("after");
      return this;
    }

    public Builder allowCached(Boolean allowCached) {
      this.allowCached = allowCached;
      this.fieldsSet.add("allowCached");
      return this;
    }

    public Builder queryName(String queryName) {
      this.queryName = queryName;
      return this;
    }
  }
}
