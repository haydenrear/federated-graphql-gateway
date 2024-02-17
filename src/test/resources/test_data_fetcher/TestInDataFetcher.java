package com.netflix.gateway.generated.datafetchers;

import com.netflix.gateway.generated.types.TestIn;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;

@DgsComponent
public class TestInDatafetcher {
    public TestInDatafetcher() {
    }

    @DgsData(
            parentType = "Query",
            field = "testIn"
    )
    public List<TestIn> getTestIn(DataFetchingEnvironment var1) {
        return List.of(new TestIn(1));
    }
}
