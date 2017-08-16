package ru.shishmakov.core;

import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.util.Times;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static ru.shishmakov.core.LifeCycle.*;
import static ru.shishmakov.util.Threads.STOP_TIMEOUT_SEC;

/**
 * @author Dmitriy Shishmakov on 24.03.17
 */
public class ServiceController {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Predicate<TimeTask> TIME_TASK_PREDICATE = buildTimeTaskPredicate();

    private final AtomicReference<LifeCycle> SERVICES_STATE = new AtomicReference<>(IDLE);
    private final PredictableQueue<TimeTask> queue;
    private final ExecutorService executor;
    private Producer producer;
    private List<Consumer> consumers;

    public ServiceController() {
        this.queue = new PredictableQueue<>(TIME_TASK_PREDICATE);
        this.executor = Executors.newCachedThreadPool();
    }

    public void startServices() {
        logger.info("Services starting...");
        final LifeCycle state = SERVICES_STATE.get();
        if (LifeCycle.isNotIdle(state)) {
            logger.warn("Warning! Services already started, state: {}", state);
            return;
        }

        try {
            SERVICES_STATE.set(INIT);
            this.producer = new Producer(queue);
            this.consumers = runConsumers();
        } catch (Throwable e) {
            logger.error("Exception occurred", e);
            throw new RuntimeException(e);
        } finally {
            SERVICES_STATE.set(RUN);
            logger.info("Services started, state: {}", SERVICES_STATE.get());
        }
    }

    public void stopServices() {
        logger.info("Services stopping...");
        final LifeCycle state = SERVICES_STATE.get();
        if (LifeCycle.isNotRun(state)) {
            logger.warn("Warning! Services already stopped, state: {}", state);
            return;
        }

        try {
            SERVICES_STATE.set(STOPPING);
            consumers.forEach(Consumer::stop);
            stopExecutors();
        } catch (Throwable e) {
            logger.error("Exception occurred during stopping node {}", e);
        } finally {
            SERVICES_STATE.set(IDLE);
            logger.info("Services stopped, state: {}", SERVICES_STATE.get());
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
        logger.info("Services executor stopping...");
        try {
            MoreExecutors.shutdownAndAwaitTermination(executor, STOP_TIMEOUT_SEC, SECONDS);
            logger.info("Executor services stopped");
        } catch (Exception e) {
            logger.error("Services exception occurred during stopping executor services", e);
        }
    }

    public boolean scheduleTask(LocalDateTime localDateTime, Callable<?> task) {
        return producer.schedule(localDateTime, task);
    }

    private static Predicate<TimeTask> buildTimeTaskPredicate() {
        return task -> task != null && Times.isTimeExpired(task.getScheduledTime());
    }

}
