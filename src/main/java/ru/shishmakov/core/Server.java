package ru.shishmakov.core;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ru.shishmakov.core.LifeCycle.*;
import static ru.shishmakov.util.Threads.assignThreadHook;
import static ru.shishmakov.util.Threads.sleepWithoutInterruptedAfterTimeout;

/**
 * @author Dmitriy Shishmakov on 23.03.17
 */
public class Server {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AtomicReference<LifeCycle> serverState = new AtomicReference<>(IDLE);

    private final ServiceController serviceController;

    public Server() {
        this.serviceController = new ServiceController();
    }

    public Server start() {
        logger.info("Server starting...");
        final LifeCycle state = serverState.get();
        if (LifeCycle.isNotIdle(state)) {
            logger.warn("Warning! Server already started, state: {}", state);
            return this;
        }

        try {
            try {
                serverState.set(INIT);
                startServices();
                assignThreadHook(this::stop, "server-hook-thread");
            } finally {
                serverState.set(RUN);
                logger.info("Server started, state: {}", serverState.get());
            }
        } catch (Exception e) {
            logger.error("Server exception", e);
            this.stop();
        }
        return this;
    }

    public void stop() {
        logger.info("Server stopping...");
        final LifeCycle state = serverState.get();
        if (LifeCycle.isNotRun(state)) {
            logger.warn("Warning! Server already stopped, state: {}", state);
            return;
        }

        try {
            serverState.set(STOPPING);
            stopServices();
        } finally {
            serverState.set(IDLE);
            logger.info("Server stopped, state: {}", serverState.get());
        }
    }

    public void await() throws InterruptedException {
        Thread.currentThread().setName("server-main");
        logger.info("Server thread: {} await the state: {} to stop itself", Thread.currentThread(), IDLE);
        for (long count = 0; LifeCycle.isNotIdle(serverState.get()); count++) {
            if (count % 100 == 0) logger.debug("Thread: {} is alive", Thread.currentThread());
            sleepWithoutInterruptedAfterTimeout(100, MILLISECONDS);
        }
    }

    @VisibleForTesting
    void startServices() {
        serviceController.startServices();
    }

    @VisibleForTesting
    void stopServices() {
        serviceController.stopServices();
    }

    public LifeCycle getState() {
        return serverState.get();
    }

    public boolean scheduleTask(LocalDateTime localDateTime, Callable<?> task) {
        return serviceController.scheduleTask(
                checkNotNull(localDateTime, "localDateTime is null"),
                checkNotNull(task, "task is null"));
    }
}
