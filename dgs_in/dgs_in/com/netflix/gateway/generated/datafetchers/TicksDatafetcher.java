package com.netflix.gateway.generated.datafetchers;

import com.netflix.gateway.generated.types.TicksConnection;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import graphql.schema.DataFetchingEnvironment;

@DgsComponent
public class TicksDatafetcher {
  @DgsData(
      parentType = "Query",
      field = "ticks"
  )
  public TicksConnection getTicks(DataFetchingEnvironment dataFetchingEnvironment) {
    return null;
  }
}
