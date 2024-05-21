package com.hayden.gateway.discovery.comm;

import com.hayden.graphql.models.federated.service.FederatedGraphQlServiceFetcherItemId;
import com.hayden.utilitymodule.MapFunctions;
import com.hayden.utilitymodule.assert_util.AssertUtil;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

public interface FederatedGraphQlState {

    enum StartupTask {
        CODE_REGISTRY, TYPE_DEFINITION_REGISTRY
    }

    record PreStartup(CountDownLatch countDownLatch) implements FederatedGraphQlState {
        public PreStartup() {
            this(new CountDownLatch(1));
        }
    }

    record IntermediaryStartup(EnumMap<StartupTask, CountDownLatch> completed) implements FederatedGraphQlState {}

    record PostStartup(Map<FederatedGraphQlServiceFetcherItemId.FederatedGraphQlServiceInstanceId, ServiceVisitorDelegate> serviceDelegates,
                       ReentrantLock reentrantLock,
                       AtomicBoolean dirty,
                       Map<FederatedGraphQlServiceFetcherItemId.FederatedGraphQlServiceInstanceId, DelayedService> services,
                       DelayQueue<DelayedService> callServiceAgain) implements FederatedGraphQlState {

        public PostStartup(Map<FederatedGraphQlServiceFetcherItemId.FederatedGraphQlServiceInstanceId, ServiceVisitorDelegate> serviceDelegates) {
            this(serviceDelegates, new ReentrantLock(), serviceDelegates.isEmpty() ? new AtomicBoolean(false) : new AtomicBoolean(true),
                    MapFunctions.CollectMap(serviceDelegates.entrySet().stream().map(e -> Map.entry(e.getKey(), e.getValue().delayedService()))),
                    new DelayQueue<>()
            );
            this.callServiceAgain.addAll(this.services.values());
        }

        public void registerRefresh(ServiceVisitorDelegate serviceDelegate) {
            updateServiceDelay(serviceDelegate);
            if (!this.serviceDelegates().containsKey(serviceDelegate.host())) {
                this.serviceDelegates().put(serviceDelegate.host(), serviceDelegate);
            } else {
                AssertUtil.assertTrue(
                        () -> this.serviceDelegates.containsKey(serviceDelegate.host()),
                        () -> "Service visitor did not contain service instance ID %s."
                                .formatted(serviceDelegate.host())
                ); 
                serviceDelegate.visitors()
                        .forEach((sKey, s) -> this.registerRefresh(s, serviceDelegate.host()));
            }

        }

        private void updateServiceDelay(ServiceVisitorDelegate serviceDelegate) {
            if (services.containsKey(serviceDelegate.host())) {
                var curr = services.remove(serviceDelegate.host());
                Assert.isTrue(callServiceAgain.remove(curr), "Call services unsynchronized.");
            } else {
                AssertUtil.assertTrue(
                        () -> callServiceAgain.stream()
                                .noneMatch(d -> d.host().equals(serviceDelegate.host())), 
                        "Call services unsynchronized."
                );
            }
            services.put(serviceDelegate.host(), serviceDelegate.delayedService());
            callServiceAgain.add(serviceDelegate.delayedService());
        }

        public void removeExpiredServices() {
            DelayedService poll = callServiceAgain.poll();
            while (poll != null) {
                remove(poll.host());
                poll = callServiceAgain.poll();
            }
        }

        public void remove(FederatedGraphQlServiceFetcherItemId.FederatedGraphQlServiceInstanceId serviceInstanceId) {
            var removed = this.serviceDelegates().remove(serviceInstanceId);
            removed.remove();
            removed.visitors().values().forEach(ServiceVisitorDelegate.ServiceVisitor::remove);
            dirty.set(true);
        }

        public void registerRefresh(ServiceVisitorDelegate.ServiceVisitor serviceVisitor,
                                    FederatedGraphQlServiceFetcherItemId.FederatedGraphQlServiceInstanceId id) {
            reentrantLock.lock();
            if (serviceVisitor.invalidateCurrent()) {
                removeCurrentAndAddNew(serviceVisitor, id);
            } else {
                putNewIfNotExisting(serviceVisitor, id);
            }
            dirty.set(true);
            reentrantLock.unlock();
        }

        private void putNewIfNotExisting(ServiceVisitorDelegate.ServiceVisitor serviceVisitor, FederatedGraphQlServiceFetcherItemId.FederatedGraphQlServiceInstanceId id) {
            serviceDelegates.compute(id, (key, prev) -> Optional.ofNullable(prev)
                    .filter(Predicate.not(p -> p.visitors().containsKey((serviceVisitor.visitor().getClass()))))
                    .stream()
                    .peek(p -> p.visitors().put(serviceVisitor.visitor().getClass(), serviceVisitor))
                    .findAny()
                    .orElseThrow(RuntimeException::new)
            );
        }

        private void removeCurrentAndAddNew(ServiceVisitorDelegate.ServiceVisitor serviceVisitor, FederatedGraphQlServiceFetcherItemId.FederatedGraphQlServiceInstanceId id) {
            serviceDelegates.compute(id, (key, prev) -> Optional.ofNullable(prev)
                    .map(p -> {
                        p.visitors().remove(serviceVisitor.visitor().getClass());
                        p.visitors().put(serviceVisitor.visitor().getClass(), serviceVisitor);
                        return p;
                    })
                    .orElseThrow(RuntimeException::new));
        }


    }

}
