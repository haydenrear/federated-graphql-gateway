package com.hayden.gateway;

import com.hayden.graphql.federated.client.IFederatedGraphQlClientBuilder;
import com.mongodb.assertions.Assertions;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.FileOutputStream;
import java.lang.reflect.Proxy;
import java.util.Arrays;

@Slf4j
public class TestJava {

    @SneakyThrows
    @Test
    public void testJavaProxy() {
        FileOutputStream fos = new FileOutputStream("build/test.java", true);
        IFederatedGraphQlClientBuilder close = (IFederatedGraphQlClientBuilder) Proxy.newProxyInstance(
                this.getClass().getClassLoader(),
                new Class[]{AutoCloseable.class, IFederatedGraphQlClientBuilder.class},
                (proxied, method, args) -> {
                    log.debug("Called {} on {} with args {}.", method.getName(), proxied.getClass().getSimpleName(), Arrays.toString(args));
                    if (method.getName().equals("close")) {
                        log.debug("Calling close on {} with args {}.", proxied.getClass().getSigners(), Arrays.toString(args));
                    }
                    if (method.getName().equals("toString")) {
                        return "proxy";
                    }
                    return method.invoke(fos, args);
                });
        if (close instanceof AutoCloseable c)
            c.close();
    }

}
