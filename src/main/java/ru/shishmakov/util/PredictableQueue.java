package ru.shishmakov.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dmitriy Shishmakov on 02.04.17
 */
public class PredictableQueue<E extends Comparable<E>> {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int DEFAULT_CAPACITY = 4096;

    private final Queue<E> queue;
    private final Predicate<E> predicate;
    private final ReentrantLock lock;

    public PredictableQueue(int capacity, Predicate<E> retrieveRule) {
        this.queue = new PriorityQueue<>(capacity);
        this.predicate = retrieveRule;
        this.lock = new ReentrantLock();
    }

    public PredictableQueue(int capacity) {
        this(capacity, e -> true);
    }

    public PredictableQueue() {
        this(DEFAULT_CAPACITY);
    }

    public Optional<E> poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            E item = queue.peek();
            if (predicate.test(item)) return Optional.ofNullable(queue.poll());
            else return Optional.empty();
        } finally {
            lock.unlock();
        }
    }

    public boolean offer(E e) {
        checkNotNull(e, "item is null");
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return queue.offer(e);
        } finally {
            lock.unlock();
        }
    }

    public boolean addAll(Collection<? extends E> transfer) {
        checkNotNull(transfer, "collection is null");
        if (transfer.isEmpty()) return false;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return queue.addAll(transfer);
        } finally {
            lock.unlock();
        }
    }

    public int drainTo(Collection<? super E> bag) {
        return drainTo(bag, Integer.MAX_VALUE);
    }

    public int drainTo(Collection<? super E> bag, int maxElements) {
        if (maxElements <= 0) return 0;
        final ReentrantLock lock = this.lock;
        lock.lock();
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
            lock.unlock();
        }
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }
}
