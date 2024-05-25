package com.hayden.gateway.discovery.comm;

import com.hayden.graphql.federated.FederatedGraphQlSourceProvider;
import com.hayden.graphql.federated.wiring.ReloadIndicator;
import com.hayden.graphql.models.federated.service.FederatedGraphQlServiceFetcherItemId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class FederatedGraphQlStateHolder {

    private static final AtomicReference<FederatedGraphQlState> appState = new AtomicReference<>(new FederatedGraphQlState.PreStartup());
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final ReloadIndicator reloadIndicator;

    private FederatedGraphQlSourceProvider federatedDynamicGraphQlSource;

    @Autowired @Lazy
    public void setFederatedDynamicGraphQlSource(FederatedGraphQlSourceProvider federatedDynamicGraphQlSource) {
        this.federatedDynamicGraphQlSource = federatedDynamicGraphQlSource;
    }

    public boolean doReload() {
        if (appState.get() instanceof FederatedGraphQlState.PostStartup postStartup) {
            lock.writeLock().lock();
            boolean anyReload = postStartup.serviceDelegates().values().stream()
                    .flatMap(s -> s.visitors().values().stream())
                    .anyMatch(s -> !s.registered().get());
            if (anyReload) {
                postStartup.serviceDelegates().values().stream()
                        .flatMap(s -> s.visitors().values().stream())
                        .filter(ServiceVisitorDelegate.ServiceVisitor::isReloadable)
                        .forEach(s -> s.registered().set(false));
            }
            var doReload = anyReload || postStartup.dirty().getAndSet(false);
            lock.writeLock().unlock();
            return doReload;
        }
        return false;
    }

    public Stream<ServiceVisitorDelegate.ServiceVisitor> current() {
        lock.writeLock().lock();
        if (appState.get() instanceof FederatedGraphQlState.PostStartup postStartup) {
            return postStartup.serviceDelegates().values().stream()
                    .onClose(lock.writeLock()::unlock)
                    .flatMap(s -> s.visitors().values().stream())
                    // re-register any service visitor delegates that are
                    // reloadable, where the context will be recreated, such
                    // as DGS, and then if reload is called this will be set to false
                    // again if reloadable.
                    .filter(s -> s.registered().getAndSet(true));
        }

        return Stream.empty();
    }

    public boolean awaitPreStartup() {
        if (appState.getAcquire() instanceof FederatedGraphQlState.PreStartup preStartup) {
            return awaitLatch(preStartup.countDownLatch(), 90);
        }
        return true;
    }

    public static boolean awaitLatch(CountDownLatch countDownLatch, int timeoutSeconds) {
        try {
            return countDownLatch.await(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Could not wait for countdown latch: {}.", e.getMessage());
        }
        return false;
    }

    public boolean waitForAllRegistrations() {
        if (!awaitPreStartup()) {
            log.error("Could not wait for federated pre-startup registration.");
            return false;
        }

        return !(appState.getAcquire() instanceof FederatedGraphQlState.IntermediaryStartup intermediaryStartup) ||
                intermediaryStartup.awaitForStartupTasks();
    }

    public void registerStartupTask(FederatedGraphQlState.StartupTask startupTask) {
        lock.writeLock().lock();
        if (appState.get() instanceof FederatedGraphQlState.PreStartup preStartup) {
            FederatedGraphQlState.IntermediaryStartup startup = new FederatedGraphQlState.IntermediaryStartup();
            appState.set(startup);
            startup.countdown(startupTask);
            preStartup.countDownLatch().countDown();
        } else if (appState.get() instanceof FederatedGraphQlState.IntermediaryStartup intermediaryStartup)  {
            intermediaryStartup.countdown(startupTask);
            if (intermediaryStartup.startupTasksDone()) {
                appState.set(new FederatedGraphQlState.PostStartup(new HashMap<>()));
            }
        }
        this.federatedDynamicGraphQlSource.setReload();
        lock.writeLock().unlock();
    }

    public void registerServiceDelegate(ServiceVisitorDelegate serviceDelegate) {
        lock.writeLock().lock();
        if (appState.get() instanceof FederatedGraphQlState.PostStartup postStartup) {
            postStartup.registerRefresh(serviceDelegate);
        }
        lock.writeLock().unlock();
    }

    public void remove(FederatedGraphQlServiceFetcherItemId.FederatedGraphQlServiceInstanceId serviceInstanceId) {
        lock.writeLock().lock();
        if (appState.get() instanceof FederatedGraphQlState.PostStartup postStartup) {
            postStartup.remove(serviceInstanceId);
            this.reloadIndicator.setReload();
        }
        lock.writeLock().unlock();
    }

    public void removeExpiredServices() {
        lock.writeLock().lock();
        if (appState.get() instanceof FederatedGraphQlState.PostStartup postStartup) {
            postStartup.removeExpiredServices();
        }
        lock.writeLock().unlock();
    }

    public Optional<ServiceVisitorDelegate> getService(ServiceInstance s) {
        if (appState.getAcquire() instanceof FederatedGraphQlState.PostStartup postStartup)
            return postStartup.getByServiceId(s);

        return Optional.empty();
    }
}
