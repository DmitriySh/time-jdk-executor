package ru.shishmakov.core;

import com.google.common.util.concurrent.MoreExecutors;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.BaseTest;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static ru.shishmakov.util.Threads.STOP_TIMEOUT_SEC;

/**
 * @author Dmitriy Shishmakov on 23.03.17
 */
public class ServiceTest extends BaseTest {

    private static ExecutorService executor;

    @BeforeClass
    public static void before() throws Exception {
        executor = Executors.newCachedThreadPool();
    }

    @AfterClass
    public static void after() throws Exception {
        MoreExecutors.shutdownAndAwaitTermination(executor, STOP_TIMEOUT_SEC, SECONDS);
    }

    @Test
    public void serviceShouldExecuteAllTasksByScheduleTimeAndIncomeOrder() throws InterruptedException {
        final Service service = new Service();
        service.start();
        for (int effort = 5; effort > 0; effort--) {
            final CountDownLatch latch = new CountDownLatch(4);
            final BlockingQueue<Long> completed = new LinkedBlockingQueue<>();
            final LocalDateTime firstTask = LocalDateTime.now(ZoneId.of("UTC")); // 4 order
            final LocalDateTime secondTask = firstTask.plusSeconds(1); // 3 order
            final LocalDateTime thirdTask = secondTask.plusSeconds(1); // 2 order
            final LocalDateTime zeroTask = LocalDateTime.from(firstTask);// 1 order
            final List<TestExecutableTask> tasks = Stream.of(zeroTask, thirdTask, secondTask, firstTask)
                    .map(ldt -> new TestExecutableTask(latch, ldt, completed))
                    .collect(Collectors.toList());

            tasks.forEach(t -> service.scheduleTask(t.getScheduleTime(), t));
            latch.await(7, SECONDS);

            final Long[] expected = tasks.stream().sorted().map(TestExecutableTask::getOrderId).toArray(Long[]::new);
            final List<Long> actual = new ArrayList<>();
            completed.drainTo(actual);
            assertEquals("All tasks should be executed", 0, latch.getCount());
            assertArrayEquals("Tasks should be executed in legal order", expected, actual.toArray(new Long[actual.size()]));
        }
        service.stop();
    }

    @Test
    public void serviceShouldAsyncExecuteAllTasksByScheduleTimeAndIncomeOrder() throws InterruptedException {
        int capacity = 30;
        final CountDownLatch awaitStart = new CountDownLatch(1);
        final CountDownLatch awaitComplete = new CountDownLatch(capacity);
        final BlockingQueue<Long> completed = new LinkedBlockingQueue<>();
        final List<TestExecutableTask> tasks = new ArrayList<>(capacity);

        final Service service = new Service();
        service.start();
        final LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        // past
        executor.submit(() -> {
            try {
                awaitStart.await();
            } catch (InterruptedException e) {
                logger.error("Error at the time waiting to start", e);
            }
            final LocalDateTime pastTime = now.minusSeconds(2);
            for (int count = capacity / 3; count > 0; count--) {
                final TestExecutableTask task = new TestExecutableTask(awaitComplete, pastTime, completed);
                service.scheduleTask(pastTime, task);
                tasks.add(task);
            }
        });
        // current time
        executor.submit(() -> {
            try {
                awaitStart.await();
            } catch (InterruptedException e) {
                logger.error("Error at the time waiting to start", e);
            }
            for (int count = capacity / 3; count > 0; count--) {
                final TestExecutableTask task = new TestExecutableTask(awaitComplete, now, completed);
                service.scheduleTask(now, task);
                tasks.add(task);
            }
        });
        // future
        executor.submit(() -> {
            try {
                awaitStart.await();
            } catch (InterruptedException e) {
                logger.error("Error at the time waiting to start", e);
            }
            final LocalDateTime futureTime = now.plusSeconds(2);
            for (int count = capacity / 3; count > 0; count--) {
                final TestExecutableTask task = new TestExecutableTask(awaitComplete, futureTime, completed);
                service.scheduleTask(futureTime, task);
                tasks.add(task);
            }
        });
        awaitStart.countDown();
        awaitComplete.await(7, SECONDS);
        service.stop();

        final Long[] expected = tasks.stream().sorted().map(TestExecutableTask::getOrderId).toArray(Long[]::new);
        final List<Long> actual = new ArrayList<>(capacity);
        completed.drainTo(actual);
        assertEquals("All tasks should be executed", 0, awaitComplete.getCount());
        assertArrayEquals("Tasks should be executed in legal order", expected, actual.toArray(new Long[actual.size()]));
    }

    public static class TestExecutableTask implements Callable<Void>, Comparable<TestExecutableTask> {
        private static final Logger taskLogger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

        private static final Comparator<TestExecutableTask> comparator = buildComparator();
        private static final AtomicLong orderIterator = new AtomicLong(1);
        private final long orderId;
        private final CountDownLatch latch;
        private final LocalDateTime scheduleTime;
        private final BlockingQueue<Long> queue;

        public TestExecutableTask(CountDownLatch latch, LocalDateTime scheduleTime, BlockingQueue<Long> queue) {
            this.latch = latch;
            this.scheduleTime = scheduleTime;
            this.queue = queue;
            this.orderId = orderIterator.getAndIncrement();
        }

        @Override
        public Void call() throws Exception {
            queue.offer(orderId);
            latch.countDown();
            taskLogger.debug("Execute task; orderId: {}, scheduleTime: {}, now: {}",
                    orderId, scheduleTime, LocalDateTime.now(ZoneId.of("UTC")));
            return null;
        }


        public LocalDateTime getScheduleTime() {
            return scheduleTime;
        }

        public long getOrderId() {
            return orderId;
        }

        @Override
        public String toString() {
            return "TestExecutableTask{" +
                    "orderId=" + orderId +
                    ", scheduleTime=" + scheduleTime +
                    '}';
        }

        @Override
        public int compareTo(TestExecutableTask other) {
            return comparator.compare(this, checkNotNull(other, "TestExecutableTask is null"));
        }

        private static Comparator<TestExecutableTask> buildComparator() {
            return Comparator.comparing(TestExecutableTask::getScheduleTime)
                    .thenComparing(TestExecutableTask::getOrderId);
        }
    }
}
