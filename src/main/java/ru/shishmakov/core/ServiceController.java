package ru.shishmakov.core;

import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static ru.shishmakov.core.LifeCycle.*;

/**
 * @author Dmitriy Shishmakov on 24.03.17
 */
public class ServiceController {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String NAME = "Services";
    private static final AtomicReference<LifeCycle> SERVICES_STATE = new AtomicReference<>(IDLE);
    private static final int DEFAULT_CAPACITY = 4096;
    private final PriorityBlockingQueue<TimeTask> queue;
    private final ExecutorService executor;
    private Producer producer;
    private List<Consumer> consumers;

    public ServiceController() {
        this.queue = new PriorityBlockingQueue<>(DEFAULT_CAPACITY);
        this.executor = Executors.newCachedThreadPool();
    }

    public void startServices() {
        logger.info("{} starting...", NAME);
        final LifeCycle state = SERVICES_STATE.get();
        if (LifeCycle.isNotIdle(state)) {
            logger.warn("Warning! {} already started, state: {}", NAME, state);
            return;
        }

        try {
            SERVICES_STATE.set(INIT);
            this.producer = new Producer(queue);
            this.consumers = runConsumers();
        } catch (Throwable e) {
            logger.error("Exception occurred during starting node {}", NAME.toLowerCase(), e);
            throw new RuntimeException(e);
        } finally {
            SERVICES_STATE.set(RUN);
            logger.info("Services started, state: {}", SERVICES_STATE.get());
        }
    }

    public void stopServices() {
        logger.info("{} stopping...", NAME);
        final LifeCycle state = SERVICES_STATE.get();
        if (LifeCycle.isNotRun(state)) {
            logger.warn("Warning! {} already stopped, state: {}", NAME, state);
            return;
        }

        try {
            SERVICES_STATE.set(STOPPING);
            // ???
            stopExecutors();
        } catch (Throwable e) {
            logger.error("Exception occurred during stopping node {}", NAME.toLowerCase(), e);
        } finally {
            SERVICES_STATE.set(IDLE);
            logger.info("{} stopped, state: {}", NAME, SERVICES_STATE.get());
        }
    }

    private List<Consumer> runConsumers() {
        return IntStream.rangeClosed(1, Math.max(2, Runtime.getRuntime().availableProcessors() / 2))
                .boxed()
                .map(i -> new Consumer(queue))
                .peek(c -> executor.execute(c::start))
                .collect(Collectors.toList());
    }

    private void stopExecutors() {
        logger.info("{} executor stopping...", NAME);
        try {
            MoreExecutors.shutdownAndAwaitTermination(executor, 10, SECONDS);
            logger.info("Executor {} stopped", NAME.toLowerCase());
        } catch (Exception e) {
            logger.error("{} exception occurred during stopping executor services", NAME, e);
        }
    }
}
