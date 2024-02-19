package com.hayden.gateway.discovery;

import com.hayden.gateway.graphql.Context;
import com.hayden.gateway.graphql.GraphQlServiceApiVisitor;
import com.hayden.gateway.graphql.RegistriesComposite;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.collections.impl.factory.Sets;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

@Component
@Slf4j
public class GraphQlVisitorCommunication implements GraphQlServiceApiVisitor {

    @Autowired
    private DiscoveryProperties discoveryProperties;

    private record DelayedService(String host, int discoveryPingSeconds) implements Delayed {

        @Override
        public int compareTo(@NotNull Delayed o) {
            return (int) (this.getDelay(TimeUnit.SECONDS) - o.getDelay(TimeUnit.SECONDS));
        }

        @Override
        public long getDelay(@NotNull TimeUnit unit) {
            return unit.convert(Duration.ofSeconds(discoveryPingSeconds));
        }

    }


    @Autowired
    private DiscoveryClient discoveryClient;
    @Autowired
    private GraphQlServiceProvider graphQlServiceProvider;
    @Autowired
    private MimeTypeRegistry mimeTypeRegistry;


    /**
     * TODO: does this need to be more granular, one per each GraphQlServiceApiVisitor type?
     */
    private DelayQueue<DelayedService> callServiceAgain = new DelayQueue<>();
    private ConcurrentHashMap<String, ServiceVisitorDelegate> services = new ConcurrentHashMap<>();
    private Queue<ServiceVisitorDelegate> toAdd = new ConcurrentLinkedDeque<>();
    private CountDownLatch getRegistrations = new CountDownLatch(1);

    @PostConstruct
    public void runDiscovery() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                var contains = new HashSet<String>();
                removeExpiredServices();
                Optional.ofNullable(discoveryClient.getInstances("GRAPHQL_SERVICE"))
                        .stream()
                        .flatMap(Collection::stream)
                        .filter(s -> Objects.nonNull(s.getHost()))
                        .peek(s -> contains.add(s.getHost()))
                        .filter(s -> !services.containsKey(s.getHost()))
                        .flatMap(s -> graphQlServiceProvider.getServiceVisitorDelegates(s.getHost())
                                .stream()
                        )
                        .forEach(this::putServices);

                pushToQueue(contains);

                if (getRegistrations.getCount() > 0)
                    getRegistrations.countDown();
            }

            private void putServices(ServiceVisitorDelegate service) {
                services.put(service.host(), service);
                callServiceAgain.add(new DelayedService(service.host(), discoveryProperties.getDiscoveryPingSeconds()));
            }

        }, 0, 100);
    }

    @Override
    public void visit(RegistriesComposite registries, Context.RegistriesContext context) {
        waitForInitialRegistration();
        while (!toAdd.isEmpty()) {
            var s = toAdd.poll();
            s.visitors().forEach(g -> g.visit(registries, context));
        }
    }

     public boolean doReload() {
        return this.toAdd.size() != 0;
    }

    @SneakyThrows
    public void waitForInitialRegistration() {
        if (!getRegistrations.await(15000, TimeUnit.MILLISECONDS)) {
            log.error("Could not wait for registration.");
        }
    }

    private void removeExpiredServices() {
        DelayedService poll = callServiceAgain.poll();
        while (poll != null) {
            services.remove(poll.host);
            poll = callServiceAgain.poll();
        }
    }

    private void pushToQueue(Set<String> contains) {
        Set<String> intersected = Sets.intersect(services.keySet(), contains);
        intersected.stream()
                .map(services::get)
                .forEach(toAddService -> toAdd.add(toAddService));
    }


}
