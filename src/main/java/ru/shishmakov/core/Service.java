package ru.shishmakov.core;

import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.util.Times;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static java.util.concurrent.TimeUnit.SECONDS;
import static ru.shishmakov.util.Threads.STOP_TIMEOUT_SEC;

/**
 * @author Dmitriy Shishmakov on 24.03.17
 */
public class Service {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Predicate<TimeTask> TIME_TASK_PREDICATE = buildTimeTaskPredicate();

    private final AtomicBoolean watcherState = new AtomicBoolean(true);
    private final PredictableQueue<TimeTask> queue;
    private final ExecutorService executor;

    public Service() {
        this.queue = new PredictableQueue<>(TIME_TASK_PREDICATE);
        this.executor = Executors.newCachedThreadPool();
    }

    public void start() {
        logger.info("Services starting...");
        try {
            executor.execute(this::process);
        } finally {
            logger.info("Services started");
        }
    }

    public void stop() {
        logger.info("Services stopping...");
        try {
            watcherState.compareAndSet(true, false);
            stopExecutors();
        } finally {
            logger.info("Services stopped, state");
        }
    }

    public boolean scheduleTask(LocalDateTime localDateTime, Callable<?> task) {
        final TimeTask timeTask = new TimeTask(localDateTime, task);
        if (queue.offer(timeTask)) {
            logger.debug("-->  put task \'{}\'", timeTask);
            return true;
        } else {
            logger.debug("X--X  reject task \'{}\'", timeTask);
            return false;
        }
    }

    private void process() {
        while (watcherState.get() && !Thread.currentThread().isInterrupted()) {
//            Threads.sleepWithInterruptedAfterTimeout(config.elevatorIntervalMs(), MILLISECONDS);
//
//            if (!consoleCommands.isEmpty()) {
//                fileLogger.info("command: {}", consoleCommands.peek().getDescription());
//            }
        }
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

    private static Predicate<TimeTask> buildTimeTaskPredicate() {
        return task -> task != null && Times.isTimeExpired(task.getScheduledTime());
    }

}
