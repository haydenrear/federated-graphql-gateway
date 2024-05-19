package com.hayden.gateway.discovery.comm;

import com.hayden.graphql.federated.FederatedGraphQlSourceProvider;
import com.hayden.graphql.federated.wiring.ReloadIndicator;
import com.hayden.graphql.models.federated.service.FederatedGraphQlServiceFetcherItemId;
import com.hayden.utilitymodule.MapFunctions;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

@Component
public class FederatedGraphQlStateHolder {

    private static final AtomicReference<FederatedGraphQlState> appState = new AtomicReference<>(new FederatedGraphQlState.PreStartup());
    private static final ReentrantLock lock = new ReentrantLock();

    @Autowired
    private ReloadIndicator reloadIndicator;

    private FederatedGraphQlSourceProvider federatedDynamicGraphQlSource;

    @Autowired @Lazy
    public void setFederatedDynamicGraphQlSource(FederatedGraphQlSourceProvider federatedDynamicGraphQlSource) {
        this.federatedDynamicGraphQlSource = federatedDynamicGraphQlSource;
    }

    public boolean doReload() {
        lock.lock();
        if (!(appState.get() instanceof FederatedGraphQlState.PostStartup postStartup))
            return false;
        boolean anyReload = postStartup.serviceDelegates().values().stream()
                .flatMap(s -> s.visitors().values().stream())
                .anyMatch(s -> !s.registered().get());
        if (anyReload)
            postStartup.serviceDelegates().values().stream()
                .flatMap(s -> s.visitors().values().stream())
                .filter(ServiceVisitorDelegate.ServiceVisitor::isReloadable)
                .forEach(s -> s.registered().set(false));
        lock.unlock();
        return anyReload;
    }

    public Stream<ServiceVisitorDelegate.ServiceVisitor> current() {
        lock.lock();
        if (appState.get() instanceof FederatedGraphQlState.PostStartup postStartup) {
            return postStartup.serviceDelegates().values().stream()
                    .onClose(lock::unlock)
                    .flatMap(s -> s.visitors().values().stream())
                    .filter(s -> s.registered().getAndSet(true));
        }

        return Stream.empty();
    }

    public boolean awaitPreStartup() {
        if (appState.getAcquire() instanceof FederatedGraphQlState.PreStartup preStartup) {
            return awaitLatch(preStartup.countDownLatch());
        }
        return true;
    }

    @SneakyThrows
    private static boolean awaitLatch(CountDownLatch countDownLatch) {
        return countDownLatch.await(15000, TimeUnit.SECONDS);
    }

    public boolean waitForAllRegistrations() {
        if (!awaitPreStartup()) {
            return false;
        }

        return !(appState.getAcquire() instanceof FederatedGraphQlState.IntermediaryStartup intermediaryStartup) ||
                intermediaryStartup.completed().values().stream()
                        .allMatch(FederatedGraphQlStateHolder::awaitLatch);
    }

    public void registerStartupTask(FederatedGraphQlState.StartupTask startupTask) {
        lock.lock();
        if (appState.get() instanceof FederatedGraphQlState.PreStartup preStartup) {
            var m = MapFunctions.CollectMap(
                    Arrays.stream(FederatedGraphQlState.StartupTask.values()).map(s -> Map.entry(s, new CountDownLatch(1))),
                    () -> new EnumMap<>(FederatedGraphQlState.StartupTask.class)
            );
            appState.set(new FederatedGraphQlState.IntermediaryStartup(m));
            countdown(startupTask, m);
            preStartup.countDownLatch().countDown();
        } else if (appState.get() instanceof FederatedGraphQlState.IntermediaryStartup intermediaryStartup)  {
            countdown(startupTask, intermediaryStartup.completed());
            if (intermediaryStartup.completed().values().stream().allMatch(c -> c.getCount() == 0)) {
                appState.set(new FederatedGraphQlState.PostStartup(new HashMap<>()));
            }
        }
        this.federatedDynamicGraphQlSource.setReload();
        lock.unlock();
    }

    public void registerServiceDelegate(ServiceVisitorDelegate serviceDelegate) {
        lock.lock();
        if (appState.get() instanceof FederatedGraphQlState.PostStartup postStartup) {
            postStartup.registerRefresh(serviceDelegate);
        }
        lock.unlock();
    }

    public void remove(FederatedGraphQlServiceFetcherItemId.FederatedGraphQlServiceInstanceId serviceInstanceId) {
        lock.lock();
        if (appState.get() instanceof FederatedGraphQlState.PostStartup postStartup) {
            postStartup.remove(serviceInstanceId);
            this.reloadIndicator.setReload();
        }
        lock.unlock();
    }

    public void removeExpiredServices() {
        lock.lock();
        if (appState.get() instanceof FederatedGraphQlState.PostStartup postStartup) {
            postStartup.removeExpiredServices();
        }
        lock.unlock();
    }

    private static void countdown(FederatedGraphQlState.StartupTask startupTask, EnumMap<FederatedGraphQlState.StartupTask, CountDownLatch> m) {
        m.computeIfPresent(startupTask, (s, c) -> {
            c.countDown();
            return c;
        });
    }

    public boolean isPreStartup() {
        lock.lock();
        var is = appState.get() instanceof FederatedGraphQlState.PreStartup;
        lock.unlock();
        return is;
    }

}
