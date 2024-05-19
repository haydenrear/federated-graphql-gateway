package com.netflix.gateway.generated.datafetchers;

import com.netflix.gateway.generated.types.TestIn;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import java.util.List;

@DgsComponent
public class TestInDatafetcher implements DataFetcher<TestIn> {
    public TestInDatafetcher() {
    }

    @DgsData(
            parentType = "Query",
            field = "testIn"
    )
    public TestIn getTestIn(DataFetchingEnvironment var1) {
        return new TestIn(1);
    }

    @Override
    public TestIn get(DataFetchingEnvironment environment) throws Exception {
        return getTestIn(environment);
    }
}
