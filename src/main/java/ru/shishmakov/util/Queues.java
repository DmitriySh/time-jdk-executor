package ru.shishmakov.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Dmitriy Shishmakov on 24.03.17
 */
public final class Queues {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int TIMES_DEFAULT = 11;
    private static final int DELAY_DEFAULT = 20;
    private static final ReentrantLock queueLock = new ReentrantLock(true);

    public static <T> Optional<T> poll(BlockingQueue<T> queue) {
        return poll(queue, null);
    }

    public static <T> Optional<T> poll(BlockingQueue<T> queue, Predicate<T> predicate) {
        return poll(queue, TIMES_DEFAULT, DELAY_DEFAULT, MILLISECONDS, predicate);
    }

    public static <T> Optional<T> poll(BlockingQueue<T> queue, int times, int delay, TimeUnit unit) {
        return poll(queue, times, delay, unit, null);
    }

    /**
     * @return item from the queue
     */
    public static <T> Optional<T> poll(BlockingQueue<T> queue, int times, int delay, TimeUnit unit,
                                       @Nullable Predicate<T> predicate) {
        final Callable<Optional<T>> fetching = (predicate == null)
                ? () -> doPoll(queue, delay, unit)
                : () -> doPollWithPredicate(queue, delay, unit, predicate);
        Optional<T> item = Optional.empty();
        try {
            final ReentrantLock lock = queueLock;
            lock.lockInterruptibly();
            try {
                while (times-- > 0 && !(item = fetching.call()).isPresent()) {
                    logger.trace("effort: {} X--- item is absent; delay: {}", times, delay);
                }
                logger.trace("<--- take item: {}", (item.isPresent()) ? item.get().getClass().getSimpleName() : null);
                return item;
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            logger.error("Thread: {} was interrupted", Thread.currentThread());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Queue poll exception ...", e);
        }
        return Optional.empty();
    }

    private static <T> Optional<T> doPollWithPredicate(BlockingQueue<T> queue, int delay, TimeUnit unit,
                                                       Predicate<T> predicate) throws InterruptedException {
        T item = queue.peek();
        if (predicate.test(item)) return Optional.ofNullable(queue.poll(delay, unit));
        else return Optional.empty();
    }

    private static <T> Optional<T> doPoll(BlockingQueue<T> queue, int delay, TimeUnit unit) throws InterruptedException {
        return Optional.ofNullable(queue.poll(delay, unit));
    }

    /**
     * @return true - if item inserted successfully, false otherwise
     */
    public static <T> boolean offer(BlockingQueue<T> queue, T item) {
        return offer(queue, item, TIMES_DEFAULT, DELAY_DEFAULT, MILLISECONDS);
    }

    /**
     * @return true - if item inserted successfully, false otherwise
     */
    public static <T> boolean offer(BlockingQueue<T> queue, T item, int times, int delay, TimeUnit unit) {
        try {
            boolean success = false;
            while (--times > 0 && !(success = queue.offer(item, delay, unit))) {
                logger.trace("effort: {} ---X reject item: {}; delay: {}", times, item.getClass().getSimpleName());
            }
            if (success) logger.trace("---> insert item: {}", item.getClass().getSimpleName());
            return success;
        } catch (Exception e) {
            logger.error("Queue offer exception ...", e);
        }
        return false;
    }
}
