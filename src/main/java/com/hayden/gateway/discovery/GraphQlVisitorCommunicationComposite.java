package com.hayden.gateway.discovery;

import com.google.common.collect.Lists;
import com.hayden.graphql.federated.transport.health.HealthEvent;
import com.hayden.gateway.graphql.Context;
import com.hayden.gateway.graphql.GraphQlServiceApiVisitor;
import com.hayden.gateway.graphql.RegistriesComposite;
import com.hayden.graphql.federated.wiring.ReloadIndicator;
import com.hayden.utilitymodule.result.Result;
import com.hayden.utilitymodule.result.map.ResultCollectors;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.collections.impl.factory.Sets;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class GraphQlVisitorCommunicationComposite implements GraphQlServiceApiVisitor{


    public static final String GRAPHQL_SERVICE = "GRAPHQL_SERVICE";

    private record DelayedService(String host, int discoveryPingSeconds, AtomicBoolean initialized, int initialDelay) implements Delayed {

        private DelayedService(String host, int discoveryPingSeconds) {
            this(host, discoveryPingSeconds, new AtomicBoolean(false), 15);
        }

        @Override
        public int compareTo(@NotNull Delayed o) {
            return (int) (this.getDelay(TimeUnit.SECONDS) - o.getDelay(TimeUnit.SECONDS));
        }

        @Override
        public long getDelay(@NotNull TimeUnit unit) {
            if (!this.initialized.getAndSet(true))
                return unit.convert(Duration.ofSeconds(initialDelay));

            return unit.convert(Duration.ofSeconds(discoveryPingSeconds));
        }

    }

    @Autowired
    private DiscoveryProperties discoveryProperties;
    @Autowired
    private DiscoveryClient discoveryClient;
    @Autowired
    private GraphQlServiceProvider graphQlServiceProvider;
    @Autowired
    private MimeTypeRegistry mimeTypeRegistry;
    @Autowired
    private ReloadIndicator reloadIndicator;


    /**
     * TODO: does this need to be more granular, one per each GraphQlServiceApiVisitor type?
     */
    private final DelayQueue<DelayedService> callServiceAgain = new DelayQueue<>();
    private final ConcurrentHashMap<String, ServiceVisitorDelegate> services = new ConcurrentHashMap<>();
    private final Queue<ServiceVisitorDelegate> toAdd = new ConcurrentLinkedDeque<>();
    private final CountDownLatch getRegistrations = new CountDownLatch(1);
    private final CountDownLatch loadCode = new CountDownLatch(1);

    @EventListener
    public void onApplicationEvent(@NotNull HealthEvent.FailedHealthEvent event) {
        removeService(event.getSource().host());
        this.reloadIndicator.setReload();
    }

    @PostConstruct
    public void runDiscovery() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runDiscoveryInner();
            }


        }, 0, 1000);
    }

    void runDiscoveryInner() {
        var contains = new HashSet<String>();
        removeExpiredServices();
        Optional.ofNullable(discoveryClient.getInstances(GRAPHQL_SERVICE))
                .stream()
                .flatMap(Collection::stream)
                .filter(s -> Objects.nonNull(s.getHost()))
                .peek(s -> contains.add(s.getHost()))
                .filter(s -> !services.containsKey(s.getHost()))
                .flatMap(s -> graphQlServiceProvider.getServiceVisitorDelegates(s.getHost()).stream())
                .forEach(this::putServices);

        pushToQueue(contains);

        if (getRegistrations.getCount() > 0)
            getRegistrations.countDown();
    }

    void putServices(ServiceVisitorDelegate service) {
        services.put(service.host(), service);
        callServiceAgain.add(new DelayedService(service.host(), discoveryProperties.getDiscoveryPingSeconds()));
    }

    public boolean isServicesEmpy() {
        return services.isEmpty();
    }

    public ServiceVisitorDelegate getService(String name) {
        return services.get(name);
    }


    @Override
    public void remove() {
        log.error("Received remove on parent visitor communication composite.");
    }

    @Override
    public String id() {
        return "";
    }

    @Override
    public Result<GraphQlServiceVisitorResponse, GraphQlServiceVisitorError> visit(RegistriesComposite registries, Context.RegistriesContext context) {
        waitForWasInitialRegistration();
        Collection<Result<GraphQlServiceVisitorResponse, GraphQlServiceVisitorError>> results
                = new ArrayList<>();

        while (!toAdd.isEmpty() && registries.codeRegistry() != null) {
            var s = toAdd.poll();

            results.add(
                    s.visitors().stream()
                            .map(g -> g.visit(registries, context))
                            .collect(ResultCollectors.from(
                                    new GraphQlServiceVisitorResponse(),
                                    new GraphQlServiceVisitorError())
                            )
            );

            if (loadCode.getCount() != 0)
                loadCode.countDown();
        }

        return results.stream()
                .collect(ResultCollectors.from(
                        new GraphQlServiceVisitorResponse(),
                        new GraphQlServiceVisitorError()
                ));
    }

     public boolean doReload() {
        return (this.getRegistrations.getCount() == 0 && !this.toAdd.isEmpty());
    }

    @SneakyThrows
    public boolean waitForWasInitialRegistration() {
        boolean wasCounted = getRegistrations.getCount() == 0;
        if (wasCounted)
            return true;
        if (!getRegistrations.await(45000, TimeUnit.MILLISECONDS)) {
            log.error("Could not wait for registration.");
        }


        return false;
    }

    @SneakyThrows
    public boolean waitForAllRegistrations() {
        waitForWasInitialRegistration();
        if (!loadCode.await(45000, TimeUnit.MILLISECONDS)) {
            log.error("Could not wait for registration.");
            return false;
        }

        return true;
    }

    private void removeExpiredServices() {
        DelayedService poll = callServiceAgain.poll();
        while (poll != null) {
            removeService(poll.host);
            poll = callServiceAgain.poll();
        }
    }

    private void removeService(String host) {
        ServiceVisitorDelegate delegate = services.get(host);
        delegate.remove();
        services.remove(host);
    }

    private void pushToQueue(Set<String> contains) {
        Set<String> intersected = Sets.intersect(services.keySet(), contains);
        intersected.stream()
                .map(services::get)
                .forEach(toAdd::add);
    }

}
