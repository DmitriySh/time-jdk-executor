package ru.shishmakov.core;

import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.util.Times;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ru.shishmakov.util.Threads.STOP_TIMEOUT_SEC;
import static ru.shishmakov.util.Threads.sleepInterrupted;

/**
 * @author Dmitriy Shishmakov on 24.03.17
 */
public class Service {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AtomicBoolean watcherState = new AtomicBoolean(true);
    private final PredictableQueue<TimeTask> queue;
    private final ExecutorService executor;

    public Service() {
        this.queue = new PredictableQueue<>(buildTimeTaskPredicate());
        this.executor = Executors.newCachedThreadPool();
    }

    public void start() {
        logger.info("Service starting...");

        executor.execute(this::scanScheduledTasks);
        logger.info("Service started");
    }

    public void stop() {
        logger.info("Service stopping...");

        watcherState.compareAndSet(true, false);
        stopExecutors();
        logger.info("Service stopped, state");
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

    private void scanScheduledTasks() {
        while (watcherState.get() && !Thread.currentThread().isInterrupted()) {
            if (queue.hasExpiredTask()) {
                final List<TimeTask> items = new ArrayList<>();
                queue.drainTo(items);
                if (!items.isEmpty()) executor.execute(processTimeTasks(items));
            }
            sleepInterrupted(250, MILLISECONDS);
        }
    }

    private void stopExecutors() {
        logger.info("Service executor stopping...");
        try {
            MoreExecutors.shutdownAndAwaitTermination(executor, STOP_TIMEOUT_SEC, SECONDS);
            logger.info("Executor services stopped");
        } catch (Exception e) {
            logger.error("Service exception occurred during stopping executor services", e);
        }
    }

    private Runnable processTimeTasks(List<TimeTask> timeTasks) {
        return () -> {
            for (TimeTask task : timeTasks) {
                logger.debug("<--  Consumer start process task \'{}\' ...", task);
                try {
                    task.call();
                } catch (Exception e) {
                    logger.error("X--X  Consumer: failed process task \'{}\'", task, e);
                }
            }
        };
    }

    private static Predicate<TimeTask> buildTimeTaskPredicate() {
        return task -> task != null && Times.isTimeExpired(task.getScheduledTime());
    }

}
