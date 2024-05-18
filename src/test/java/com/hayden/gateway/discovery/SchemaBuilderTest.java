package com.hayden.gateway.discovery;

import com.hayden.gateway.compile.JavaCompile;
import com.hayden.gateway.graphql.GraphQlDataFetcher;
import com.hayden.gateway.graphql.GraphQlServiceApiVisitor;
import com.hayden.gateway.graphql.GraphQlTransports;
import com.hayden.graphql.federated.transport.http.HttpGraphQlTransportBuilder;
import com.hayden.graphql.federated.transport.source.FederatedDynamicGraphQlSource;
import com.hayden.graphql.models.GraphQlTarget;
import com.hayden.graphql.models.SourceType;
import com.hayden.graphql.models.federated.service.FederatedGraphQlServiceFetcherItemId;
import com.hayden.graphql.models.visitor.*;
import com.hayden.graphql.models.visitor.datafetcher.DataFetcherGraphQlSource;
import com.hayden.graphql.models.visitor.datafetcher.DataFetcherSourceId;
import com.hayden.graphql.models.visitor.datafetcher.GraphQlDataFetcherDiscoveryModel;
import com.hayden.graphql.models.visitor.schema.GraphQlFederatedSchemaSource;
import com.hayden.graphql.models.visitor.simpletransport.GraphQlTransportModel;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.internal.DefaultDgsQueryExecutor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.dataloader.impl.Assertions;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.MimeType;

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@ActiveProfiles("deploy-test")
@ExtendWith(SpringExtension.class)
@Slf4j
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true"
})
public class SchemaBuilderTest {

    private static FederatedGraphQlServiceFetcherItemId.FederatedGraphQlServiceFetcherId id;
    private static FederatedGraphQlServiceFetcherItemId fetcherItemId;
    private static MimeType files;
    @MockBean
    private DiscoveryClient discoveryClient;
    @Autowired
    private JavaCompile dgsJavaCompile;
    @Autowired
    private DgsQueryExecutor queryExecutor;
    @Autowired
    private Discovery discovery;

    @Mock
    private ServiceInstance serviceInstance;
    @Autowired
    private FederatedDynamicGraphQlSource federatedDynamicGraphQlSource;

    @MockBean
    GraphQlServiceProvider serviceProvider;

    private final CountDownLatch downLatch = new CountDownLatch(1);



    @SuppressWarnings("unchecked")
    @SneakyThrows
    @BeforeEach
    public void beforeEach() {
        files = new MimeType("files", "*");
        id = new FederatedGraphQlServiceFetcherItemId.FederatedGraphQlServiceFetcherId(
                files,
                UUID.randomUUID().toString(),
                "fetch"
        );
        fetcherItemId = new FederatedGraphQlServiceFetcherItemId(
                id,
                "fetch",
                "localhost"
        );
//        var loaded = dgsJavaCompile.compileAndLoad(new JavaCompile.CompileArgs( "src/test/resources/graphql", "dgs_in"));
        Mockito.when(serviceInstance.getHost()).thenReturn("test");
        Mockito.when(discoveryClient.getInstances(any())).thenReturn(List.of(serviceInstance), new ArrayList<>());
        var googleProtobuf = FileUtils.readFileToString(new File("src/test/resources/test_schemas/any_pb.graphql"), Charset.defaultCharset());
        var testSchema = FileUtils.readFileToString(new File("src/test/resources/test_schemas/test.graphql"), Charset.defaultCharset());
        var fetcher = FileUtils.readFileToString(new File("src/test/resources/test_data_fetcher/TestInDataFetcher.java"), Charset.defaultCharset());
        mockServiceProvider(List.of(googleProtobuf, testSchema), fetcher);

        discovery.waitForWasInitialRegistration();

        federatedDynamicGraphQlSource.reload(true);
    }

    @SneakyThrows
    @Test
    public void testLoad() {
        waitUntilServices();
        @Language("GraphQL") String mutation = """
        mutation { addTestIn( inValue: 1 ) {  testInValue } }
        """;
        var out = queryExecutor.execute(mutation);
        log.info("Mutation result: {}", mutation);

        @Language("GraphQL") String query = """
        { testIn { testInValue } }
        """;
        out = queryExecutor.execute(query);
        Assertions.nonNull(out.getData());
        log.info("Query result: {}", out);
        assertThat(queryExecutor).isInstanceOf(DefaultDgsQueryExecutor.class);
        assertThat(((DefaultDgsQueryExecutor) queryExecutor).getSchema().get().getTypeMap())
                .containsKey("GoogleProtobuf_Any");
    }

    private void waitUntilServices() {
        assertThat(discovery.waitForWasInitialRegistration()).isTrue();
    }


    private void mockServiceProvider(List<String> schemas, String fetcher) {
        Mockito.when(serviceProvider.getServiceVisitorDelegates(any()))
                .thenReturn(Optional.of(new ServiceVisitorDelegate("test", List.of(
                        graphQlDataFetcher(schemas, fetcher),
                        graphTransport()
                ))));
    }

    private static @NotNull GraphQlDataFetcher graphQlDataFetcher(List<String> schemas, String fetcher) {
        return new GraphQlDataFetcher(
                new GraphQlDataFetcherDiscoveryModel(
                        fetcherItemId,
                        schemas.stream()
                                .map(schemaStr -> new GraphQlFederatedSchemaSource(GraphQlTarget.String, schemaStr))
                                .toList(),
                        List.of(
                                new DataFetcherGraphQlSource(
                                        "TestIn",
                                        Map.of(new DataFetcherSourceId("TestIn", SourceType.DgsComponentJava, "testIn", files),
                                                new DataSource("TestIn", GraphQlTarget.String, fetcher))
                                ))
                ),
                "test"
        );
    }

//    private static @NotNull GraphQlDataFederation graphQlDataFederation() {
//        return new GraphQlDataFederation(
//                new GraphQlDataFederationModel(fetcherItemId, )
//        );
//    }

    private static @NotNull GraphQlTransports graphTransport() {
        return new GraphQlTransports(new GraphQlTransportModel(
                fetcherItemId,
                HttpGraphQlTransportBuilder.builder()
                        .host("localhost")
                        .port(8080)
                        .path("/graphql")
                        .build()
        ));
    }

}
