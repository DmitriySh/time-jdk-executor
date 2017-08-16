package ru.shishmakov.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dmitriy Shishmakov on 24.03.17
 */
public class TimeTask implements Callable, Comparable<TimeTask> {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Comparator<TimeTask> TT_COMPARATOR = buildTaskTimeComparator();
    private static final AtomicInteger orderIterator = new AtomicInteger(1);

    private final int orderId;
    private final long scheduledTime;
    private final Callable<?> task;

    public TimeTask(LocalDateTime localDateTime, Callable<?> task) {
        this.orderId = orderIterator.getAndIncrement();
        this.scheduledTime = localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
        this.task = task;
    }

    public int getOrderId() {
        return orderId;
    }

    public long getScheduledTime() {
        return scheduledTime;
    }

    public Callable<?> getTask() {
        return task;
    }

    @Override
    public Void call() throws Exception {
        task.call();
        return null;
    }

    @Override
    public String toString() {
        return "TimeTask{" +
                "orderId=" + orderId +
                ", scheduledTime=" + scheduledTime +
                '}';
    }

    @Override
    public int compareTo(TimeTask other) {
        return TT_COMPARATOR.compare(this, checkNotNull(other, "Task is null"));
    }

    private static Comparator<TimeTask> buildTaskTimeComparator() {
        return Comparator.comparing(TimeTask::getScheduledTime)
                .thenComparing(TimeTask::getOrderId);
    }
}
