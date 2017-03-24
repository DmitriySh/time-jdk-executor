package ru.shishmakov.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ru.shishmakov.concurrent.Threads.assignThreadHook;
import static ru.shishmakov.concurrent.Threads.sleepWithoutInterruptedAfterTimeout;
import static ru.shishmakov.core.LifeCycle.*;

/**
 * @author Dmitriy Shishmakov on 23.03.17
 */
public class Server {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String NAME = MethodHandles.lookup().lookupClass().getSimpleName();
    private static final AtomicReference<LifeCycle> SERVER_STATE = new AtomicReference<>(IDLE);

    LifeCycle getState() {
        return SERVER_STATE.get();
    }

    public Server startAsync() {
        new Thread(this::start, NAME).start();
        return this;
    }

    public Server start() {
        logger.info("{} starting...", NAME);
        final LifeCycle state = SERVER_STATE.get();
        if (LifeCycle.isNotIdle(state)) {
            logger.warn("Warning! {} already started, state: {}", NAME, state);
            return this;
        }

        assignThreadHook(this::stop, NAME + "-hook-thread");
        SERVER_STATE.set(RUN);
        logger.info("{} started, state: {}", NAME, SERVER_STATE.get());
        return this;
    }

    public void stop() {
        logger.info("{} stopping...", NAME);
        final LifeCycle state = SERVER_STATE.get();
        if (LifeCycle.isNotRun(state)) {
            logger.warn("Warning! {} already stopped, state: {}", NAME, state);
            return;
        }

        try {
            SERVER_STATE.set(STOPPING);
            stopExecutors();
        } finally {
            SERVER_STATE.set(IDLE);
            logger.info("{} stopped, state: {}", NAME, SERVER_STATE.get());
        }
    }

    public void await() {
        Thread.currentThread().setName(NAME + "-main");
        logger.info("{} thread: {} await the state: {} to stop itself", NAME, Thread.currentThread(), IDLE);
        for (long count = 0; LifeCycle.isNotIdle(SERVER_STATE.get()); count++) {
            if (count % 100 == 0) logger.debug("Thread: {} is alive", Thread.currentThread());
            sleepWithoutInterruptedAfterTimeout(100, MILLISECONDS);
        }
    }

    private void stopExecutors() {
        logger.info("{} executor services stopping...", NAME);
        try {
//            MoreExecutors.shutdownAndAwaitTermination(executor, STOP_TIMEOUT_SEC, SECONDS);
            logger.info("Executor services stopped");
        } catch (Exception e) {
            logger.error("{} exception occurred during stopping executor services", NAME, e);
        }
    }
}
