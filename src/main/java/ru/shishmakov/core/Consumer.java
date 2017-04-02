package ru.shishmakov.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.util.Queues;
import ru.shishmakov.util.Times;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ru.shishmakov.util.Threads.sleepInterrupted;

/**
 * @author Dmitriy Shishmakov on 24.03.17
 */
public class Consumer {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String NAME = MethodHandles.lookup().lookupClass().getSimpleName();
    private static final Predicate<TimeTask> timeExpired = buildTimeTaskPredicate();
    private static final AtomicInteger numberIterator = new AtomicInteger(1);
    private final int selfNumber = numberIterator.getAndIncrement();
    private final AtomicBoolean consumerState = new AtomicBoolean(true);
    private final CountDownLatch awaitStop = new CountDownLatch(1);
    private final BlockingQueue<TimeTask> queue;

    public Consumer(BlockingQueue<TimeTask> queue) {
        this.queue = queue;
    }

    protected void start() {
        logger.info("{}:{} started", NAME, selfNumber);
        try {
            while (consumerState.get() && !Thread.currentThread().isInterrupted()) {
                Queues.poll(queue, timeExpired).ifPresent(t -> {
                    logger.debug("<--  {}:{} start process task \'{}\' ...", NAME, selfNumber, t);
                    try {
                        t.call();
                    } catch (Exception e) {
                        logger.error("X--X  {}:{} failed process task '{}'", NAME, selfNumber, e);
                    }
                });
                sleepInterrupted(250, MILLISECONDS);
            }
        } catch (Exception e) {
            logger.error("{}:{} error in time of processing", NAME, selfNumber, e);
        } finally {
            shutdownConsumer();
            awaitStop.countDown();
        }
    }

    protected void stop() {
        logger.info("{}:{} stopping...", NAME, selfNumber);
        try {
            shutdownConsumer();
            awaitStop.await(2, SECONDS);
            logger.info("{}:{} stopped", NAME, selfNumber);
        } catch (Exception e) {
            logger.error("{}:{} error in time of stopping", NAME, selfNumber, e);
        }
    }

    private void shutdownConsumer() {
        if (consumerState.compareAndSet(true, false)) {
            logger.debug("{}:{} waiting for shutdown process to complete...", NAME, selfNumber);
        }
    }

    private static Predicate<TimeTask> buildTimeTaskPredicate() {
        return task -> task != null && Times.isTimeExpired(task.getScheduledTime());
    }
}
