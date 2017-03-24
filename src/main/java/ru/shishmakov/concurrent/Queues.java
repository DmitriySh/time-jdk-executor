package ru.shishmakov.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Dmitriy Shishmakov on 24.03.17
 */
public final class Queues {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int TIMES_DEFAULT = 11;
    private static final int DELAY_DEFAULT = 20;

    public static <T> Optional<T> poll(BlockingQueue<T> queue) {
        return poll(queue, TIMES_DEFAULT, DELAY_DEFAULT, MILLISECONDS);
    }

    /**
     * @return true - if item inserted successfully, false otherwise
     */
    public static <T> boolean offer(BlockingQueue<T> queue, T item) {
        return offer(queue, item, TIMES_DEFAULT, DELAY_DEFAULT, MILLISECONDS);
    }

    /**
     * @return item from the queue
     */
    public static <T> Optional<T> poll(BlockingQueue<T> queue, int times, int delay, TimeUnit unit) {
        try {
            T item = null;
            while (times-- > 0 && (item = queue.poll(delay, unit)) == null) {
                logger.trace("effort: {} X--- item is absent; delay: {}", times, delay);
            }
            logger.trace("<--- take item: {}", (item == null) ? null : item.getClass().getSimpleName());
            return Optional.ofNullable(item);
        } catch (Exception e) {
            logger.error("Queue poll exception ...", e);
            return Optional.empty();
        }
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
