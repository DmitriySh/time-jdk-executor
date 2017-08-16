package ru.shishmakov.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ru.shishmakov.util.Threads.sleepInterrupted;

/**
 * @author Dmitriy Shishmakov on 24.03.17
 */
public class Consumer {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final AtomicInteger numberIterator = new AtomicInteger(1);
    private final int selfNumber = numberIterator.getAndIncrement();
    private final AtomicBoolean consumerState = new AtomicBoolean(true);
    private final PredictableQueue<TimeTask> queue;

    public Consumer(PredictableQueue<TimeTask> queue) {
        this.queue = queue;
    }

    protected void start() {
        logger.info("Consumer: {} started", selfNumber);
        try {
            while (consumerState.get() && !Thread.currentThread().isInterrupted()) {
                final List<TimeTask> items = new ArrayList<>();
                queue.drainTo(items);
                items.forEach(t -> {
                    logger.debug("<--  Consumer: {} start process task \'{}\' ...", selfNumber, t);
                    try {
                        t.call();
                    } catch (Exception e) {
                        logger.error("X--X  Consumer: {} failed process task '{}'", selfNumber, e);
                    }
                });
                sleepInterrupted(250, MILLISECONDS);
            }
        } catch (Exception e) {
            logger.error("Consumer: {} error in time of processing", selfNumber, e);
        } finally {
            if (consumerState.compareAndSet(true, false)) {
                logger.debug("Consumer: {} waiting for shutdown process to complete...", selfNumber);
            }
        }
    }

}
