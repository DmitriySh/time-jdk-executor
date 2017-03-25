package ru.shishmakov.core;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dmitriy Shishmakov on 24.03.17
 */
public class TimeTask implements Callable, Comparable<TimeTask> {
    private static final Comparator<TimeTask> TT_COMPARATOR = buildTaskTimeComparator();

    private static final String NAME = MethodHandles.lookup().lookupClass().getSimpleName();
    private final long orderId;
    private final long scheduledTime;
    private final Callable<?> task;

    public TimeTask(long orderId, LocalDateTime localDateTime, Callable<?> task) {
        this.orderId = orderId;
        this.scheduledTime = localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
        this.task = task;
    }

    public long getOrderId() {
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
        return TT_COMPARATOR.compare(this, checkNotNull(other, "{} is null", NAME));
    }

    private static Comparator<TimeTask> buildTaskTimeComparator() {
        return Comparator.comparing(TimeTask::getScheduledTime)
                .thenComparing(TimeTask::getOrderId);
    }
}
