package com.netflix.gateway.generated.datafetchers;

import com.netflix.gateway.generated.types.GoogleProtobuf_Any;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

@DgsComponent
public class TestGoogleProtobuf implements DataFetcher<GoogleProtobuf_Any> {
    public TestGoogleProtobuf() {
    }

    @DgsData.List({
            @DgsData(parentType = "Query",
                     field = "testProto"),
            @DgsData(parentType = "DataFetcherValue",
                     field = "getProtoBuf"),
    })
    public GoogleProtobuf_Any getTestProto(DataFetchingEnvironment var1) {
        return new GoogleProtobuf_Any("hello", "hello");
    }

    @Override
    public GoogleProtobuf_Any get(DataFetchingEnvironment environment) throws Exception {
        return getTestProto(environment);
    }
}
