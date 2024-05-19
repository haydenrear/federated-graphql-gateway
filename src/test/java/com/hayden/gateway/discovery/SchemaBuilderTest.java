package com.hayden.gateway.discovery;

import com.hayden.gateway.compile.FlyJavaCompile;
import com.hayden.gateway.discovery.comm.DelayedService;
import com.hayden.gateway.discovery.comm.GraphQlServiceProvider;
import com.hayden.gateway.discovery.comm.GraphQlVisitorCommunicationComposite;
import com.hayden.gateway.discovery.comm.ServiceVisitorDelegate;
import com.hayden.gateway.graphql.GraphQlDataFetcher;
import com.hayden.gateway.graphql.GraphQlTransports;
import com.hayden.graphql.federated.transport.http.HttpGraphQlTransportBuilder;
import com.hayden.graphql.federated.transport.source.FederatedDynamicGraphQlSource;
import com.hayden.graphql.models.GraphQlTarget;
import com.hayden.graphql.models.SourceType;
import com.hayden.graphql.models.federated.service.FederatedGraphQlServiceFetcherItemId;
import com.hayden.graphql.models.visitor.datafetcher.DataFetcherGraphQlSource;
import com.hayden.graphql.models.visitor.datafetcher.DataFetcherSourceId;
import com.hayden.graphql.models.visitor.datafetcher.GraphQlDataFetcherDiscoveryModel;
import com.hayden.graphql.models.visitor.model.DataSource;
import com.hayden.graphql.models.visitor.model.Digest;
import com.hayden.graphql.models.visitor.schema.GraphQlFederatedSchemaSource;
import com.hayden.graphql.models.visitor.simpletransport.GraphQlTransportModel;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.internal.DefaultDgsQueryExecutor;
import graphql.ExecutionResult;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MimeType;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Slf4j
@ActiveProfiles("deploy-test")
@TestPropertySource(properties = {"spring.main.allow-bean-definition-overriding=true"})
public class SchemaBuilderTest {

    private static FederatedGraphQlServiceFetcherItemId.FederatedGraphQlServiceFetcherId id;
    private static FederatedGraphQlServiceFetcherItemId fetcherItemId;
    private static MimeType files;
    @MockBean
    private DiscoveryClient discoveryClient;
    @Autowired
    private FlyJavaCompile dgsFlyCompileJava;
    @Autowired
    private DgsQueryExecutor queryExecutor;
    @Autowired
    private Discovery discovery;

    @Mock
    private ServiceInstance serviceInstance;
    @Autowired
    private FederatedDynamicGraphQlSource federatedDynamicGraphQlSource;
    @Autowired
    private GraphQlVisitorCommunicationComposite visitorCommunicationComposite;

    @MockBean
    GraphQlServiceProvider serviceProvider;



    private static FederatedGraphQlServiceFetcherItemId.FederatedGraphQlServiceId serviceId;
    private static FederatedGraphQlServiceFetcherItemId.FederatedGraphQlServiceInstanceId serviceInstanceId;
    private static FederatedGraphQlServiceFetcherItemId.FederatedGraphQlServiceFetcherId federated;
    private static FederatedGraphQlServiceFetcherItemId.FederatedGraphQlHost host = new FederatedGraphQlServiceFetcherItemId.FederatedGraphQlHost("localhost");

    static {
        files = new MimeType("files", "*");
        serviceId
                = new FederatedGraphQlServiceFetcherItemId.FederatedGraphQlServiceId("fetch");
        federated
                = new FederatedGraphQlServiceFetcherItemId.FederatedGraphQlServiceFetcherId(files, "fetch", serviceId);
        serviceInstanceId
                = new FederatedGraphQlServiceFetcherItemId.FederatedGraphQlServiceInstanceId(serviceId, host);
    }


    @SuppressWarnings("unchecked")
    @SneakyThrows
    @BeforeEach
    public void beforeEach() {
        com.hayden.utilitymodule.io.FileUtils.deleteFilesRecursive(Paths.get("dgs_in/com"));
        Path path = Paths.get("schema-in");
        com.hayden.utilitymodule.io.FileUtils.deleteFilesRecursive(path);
        path.toFile().mkdir();
        com.hayden.utilitymodule.io.FileUtils.deleteFilesRecursive(Paths.get("build/classes/java/main/com/netflix"));
        id = new FederatedGraphQlServiceFetcherItemId.FederatedGraphQlServiceFetcherId(
                files,
                UUID.randomUUID().toString(),
                serviceId
        );
        fetcherItemId = new FederatedGraphQlServiceFetcherItemId(
                id,
                serviceInstanceId
        );

        Mockito.when(serviceInstance.getHost()).thenReturn("test");
        Mockito.when(discoveryClient.getInstances(any())).thenReturn(List.of(serviceInstance), new ArrayList<>());

        var googleProtobuf = FileUtils.readFileToString(new File("src/test/resources/test_schemas/any_pb.graphql"), Charset.defaultCharset());
        var testSchema = FileUtils.readFileToString(new File("src/test/resources/test_schemas/test.graphql"), Charset.defaultCharset());
        var fetcher = FileUtils.readFileToString(new File("src/test/resources/test_data_fetcher/TestInDatafetcher.java"), Charset.defaultCharset());

        mockServiceProvider(List.of(googleProtobuf, testSchema), fetcher);


        assertThat(discovery.waitForWasInitialRegistration()).isTrue();

        federatedDynamicGraphQlSource.reload(true);
        ReflectionTestUtils.invokeMethod(visitorCommunicationComposite, "runDiscoveryInner");
        queryExecutor.execute("");
        assertThat(queryExecutor).isInstanceOf(DefaultDgsQueryExecutor.class);
    }

    @SneakyThrows
    @Test
    public void testCompileLoadFetch() {


        @Language("GraphQL") String mutation = """
        mutation { addTestIn( inValue: 1 ) {  testInValue } }
        """;

        var out = queryExecutor.execute(mutation);
        @Language("GraphQL") String testInQuery = "{ testIn { testInValue } }";
        assertThat(((DefaultDgsQueryExecutor) queryExecutor).getSchema().get().getTypeMap())
                .containsKey("GoogleProtobuf_Any");

        Assertions.nonNull(out.getData());
        doValidate(testInQuery);
    }

    private void doValidate(@Language("GraphQL") String testInQuery) {
        ExecutionResult execute = queryExecutor.execute(testInQuery);
        Map<String, Map> data = execute.getData();
        assertThat(data.get("testIn").get("testInValue")).isEqualTo(1);
    }

    @SneakyThrows
    private void mockServiceProvider(List<String> schemas, String fetcher) {
        Mockito.when(serviceProvider.getServiceVisitorDelegates(any()))
                .thenReturn(Optional.of(
                        new ServiceVisitorDelegate(serviceInstanceId,
                                Map.of(
                                        GraphQlDataFetcher.class, new ServiceVisitorDelegate.ServiceVisitor(graphQlDataFetcher(schemas, fetcher)),
                                        GraphQlTransports.class, new ServiceVisitorDelegate.ServiceVisitor(graphTransport())
                                ),
                                new DelayedService(serviceInstanceId, 100),
                                createDigest("hello")
                        ))
                );
    }

    @SneakyThrows
    private static Digest.MessageDigestBytes createDigest(String hello) {
        return new Digest.MessageDigestBytes(MessageDigest.getInstance("MD5").digest(hello.getBytes()));
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
                                        "TestInDatafetcher",
                                        Map.of(new DataFetcherSourceId("TestInDatafetcher", SourceType.DgsComponentJava, "testIn", files),
                                                new DataSource("TestInDatafetcher", "com.netflix.gateway.generated.datafetchers", GraphQlTarget.String, fetcher))
                                )),
                        true
                ),
                createDigest("goodbye")
        );
    }

    private static @NotNull GraphQlTransports graphTransport() {
        return new GraphQlTransports(
                new GraphQlTransportModel(
                        fetcherItemId,
                        HttpGraphQlTransportBuilder.builder()
                                .host("localhost")
                                .port(8080)
                                .path("/graphql")
                                .build(),
                        serviceInstanceId,
                        true
                ),
                createDigest("hello"));
    }

}
