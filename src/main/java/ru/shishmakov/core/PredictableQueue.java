package ru.shishmakov.core;

import java.util.Collection;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dmitriy Shishmakov on 17.08.17
 */
public class PredictableQueue<E extends Comparable<E>> {
    private static final int DEFAULT_CAPACITY = 4096;

    private final Queue<E> queue;
    private final Predicate<E> predicate;
    private final AtomicBoolean lock;

    public PredictableQueue(int capacity, Predicate<E> retrieveRule) {
        this.queue = new PriorityQueue<>(capacity);
        this.predicate = retrieveRule;
        this.lock = new AtomicBoolean(false);
    }

    public PredictableQueue(Predicate<E> retrieveRule) {
        this(DEFAULT_CAPACITY, retrieveRule);
    }

    public Optional<E> poll() {
        while (!lock.compareAndSet(false, true)) {/*nothing to do*/}
        try {
            E item = queue.peek();
            if (predicate.test(item)) return Optional.ofNullable(queue.poll());
            else return Optional.empty();
        } finally {
            lock.set(false);
        }
    }

    public boolean offer(E e) {
        checkNotNull(e, "item is null");
        while (!lock.compareAndSet(false, true)) {/*nothing to do*/}
        try {
            return queue.offer(e);
        } finally {
            lock.set(false);
        }
    }

    public boolean addAll(Collection<? extends E> transfer) {
        checkNotNull(transfer, "collection is null");
        if (transfer.isEmpty()) return false;
        while (!lock.compareAndSet(false, true)) {/*nothing to do*/}
        try {
            return queue.addAll(transfer);
        } finally {
            lock.set(false);
        }
    }

    public boolean hasExpiredTask() {
        E item = queue.peek();
        return predicate.test(item);
    }

    public int drainTo(Collection<? super E> bag) {
        return drainTo(bag, Integer.MAX_VALUE);
    }

    public int drainTo(Collection<? super E> bag, int maxElements) {
        if (maxElements <= 0) return 0;
        while (!lock.compareAndSet(false, true)) {/*nothing to do*/}
        try {
            int quantity = 0;
            while (maxElements-- > 0 && !queue.isEmpty()) {
                E item = queue.peek();
                if (predicate.test(item)) {
                    bag.add(queue.poll());
                    quantity++;
                } else break;
            }
            return quantity;
        } finally {
            lock.set(false);
        }
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }
}
