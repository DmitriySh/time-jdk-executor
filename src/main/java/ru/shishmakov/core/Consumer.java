package ru.shishmakov.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.concurrent.Queues;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ru.shishmakov.concurrent.Threads.sleepInterrupted;

/**
 * @author Dmitriy Shishmakov on 24.03.17
 */
public class Consumer {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final AtomicInteger numberIterator = new AtomicInteger();
    private final int selfNumber = numberIterator.incrementAndGet();
    private final AtomicBoolean consumerState = new AtomicBoolean(true);
    private final CountDownLatch awaitStop = new CountDownLatch(1);
    private final BlockingQueue<TimeTask> queue;

    public Consumer(BlockingQueue<TimeTask> queue) {
        this.queue = queue;
    }

    protected void start() {
        logger.info("Consumer:{} started", selfNumber);
        try {
            while (consumerState.get() && !Thread.currentThread().isInterrupted()) {
                Queues.poll(queue).ifPresent(t -> {
                    logger.debug("<--  Consumer:{} start process task \'{}\' ...", selfNumber, t);
                    try {
                        t.call();
                    } catch (Exception e) {
                        logger.error("X--X  Consumer:{} failed process task '{}'", selfNumber, e);
                    }
                });
                sleepInterrupted(250, MILLISECONDS);
            }
        } catch (Exception e) {
            logger.error("Consumer:{} error in time of processing", selfNumber, e);
        } finally {
            shutdownConsumer();
            awaitStop.countDown();
        }
    }

    protected void stop() {
        logger.info("Consumer:{} stopping...", selfNumber);
        try {
            shutdownConsumer();
            awaitStop.await(2, SECONDS);
            logger.info("Consumer:{} stopped", selfNumber);
        } catch (Exception e) {
            logger.error("Consumer:{} error in time of stopping", selfNumber, e);
        }
    }

    private void shutdownConsumer() {
        if (consumerState.compareAndSet(true, false)) {
            logger.debug("Consumer:{} waiting for shutdown process to complete...", selfNumber);
        }
    }
}
