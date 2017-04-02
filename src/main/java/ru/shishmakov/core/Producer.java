package ru.shishmakov.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Dmitriy Shishmakov on 24.03.17
 */
public class Producer {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String NAME = MethodHandles.lookup().lookupClass().getSimpleName();
    private static final AtomicInteger orderIterator = new AtomicInteger(1);
    private final PredictableQueue<TimeTask> queue;

    public Producer(PredictableQueue<TimeTask> queue) {
        this.queue = queue;
    }

    public boolean schedule(LocalDateTime localDateTime, Callable<?> task) {
        final TimeTask timeTask = new TimeTask(orderIterator.getAndIncrement(), localDateTime, task);
        if (queue.offer(timeTask)) {
            logger.debug("-->  {} put task \'{}\'", NAME, timeTask);
            return true;
        } else {
            logger.debug("X--X  {} reject task \'{}\'", NAME, timeTask);
            return false;
        }
    }
}
