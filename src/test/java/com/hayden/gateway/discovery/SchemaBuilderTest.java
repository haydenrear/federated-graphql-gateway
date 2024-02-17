package com.hayden.gateway.discovery;

import com.hayden.gateway.compile.JavaCompile;
import com.hayden.gateway.graphql.GraphQlServiceRegistration;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.internal.DefaultDgsQueryExecutor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.dataloader.impl.Assertions;
import org.intellij.lang.annotations.Language;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@TestPropertySource(
        properties = {
                "compiler.in.path=src/test/resources"
        }
)
@Slf4j
public class SchemaBuilderTest {

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

    @MockBean
    GraphQlServiceProvider serviceProvider;

    private final CountDownLatch downLatch = new CountDownLatch(1);

    @SuppressWarnings("unchecked")
    @SneakyThrows
    @BeforeEach
    public void beforeEach() {
        var loaded = dgsJavaCompile.compileAndLoad(new JavaCompile.CompileArgs( "src/test/resources/graphql", "dgs_in"));
        Mockito.when(serviceInstance.getHost()).thenReturn("test");
        Mockito.when(discoveryClient.getInstances(any())).thenReturn(List.of(serviceInstance), new ArrayList<>());
        var fs = FileUtils.readFileToString(new File("src/main/resources/graphql/any_pb.graphql"), Charset.defaultCharset());
        var test = FileUtils.readFileToString(new File("src/main/resources/graphql/test.graphql"), Charset.defaultCharset());
        var fetcher = FileUtils.readFileToString(new File("src/test/resources/test_data_fetcher/TestInDataFetcher.txt"), Charset.defaultCharset());
        mockServiceProvider(fs, test, fetcher);
    }

    private void mockServiceProvider(String fs, String test, String fetcher) {
        Mockito.when(serviceProvider.getServiceVisitorDelegates(any(), any()))
                .thenReturn(Optional.of(new GraphQlServiceProvider.ServiceVisitorDelegate("test", List.of(
                        new GraphQlServiceRegistration(
                                "test",
                                new GraphQlServiceRegistration.GraphQlFederatedSchemaSource(
                                        Map.of(GraphQlServiceRegistration.GraphQlTarget.String, List.of(fs, test))
                                ),
                                List.of(
                                        new GraphQlServiceRegistration.GraphQlFetcherSource(
                                                "TestIn",
                                                Map.of(new GraphQlServiceRegistration.DataFetcherSourceId(GraphQlServiceRegistration.DataFetcherSourceType.DgsComponentJava, "testIn"),
                                                       new GraphQlServiceRegistration.DataFetcherSource(GraphQlServiceRegistration.GraphQlTarget.String, fetcher))
                                        ))
                        )
                ))));
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

    private void waitUntilServices() throws InterruptedException {
        while (((ConcurrentHashMap<String, GraphQlServiceRegistration>) ReflectionTestUtils.getField(discovery, "services")).isEmpty())
            Thread.sleep(30);
        queryExecutor.execute("");
    }
}
