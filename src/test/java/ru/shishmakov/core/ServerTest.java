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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static ru.shishmakov.util.Threads.STOP_TIMEOUT_SEC;

/**
 * @author Dmitriy Shishmakov on 23.03.17
 */
public class ServerTest extends BaseTest {

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
    public void afterStartServerShouldHasRunState() {
        final Server server = spy(new Server());
        doNothing().when(server).startServices();
        doNothing().when(server).stopServices();

        server.start();

        assertEquals("Server after start should be in " + RUN + " state", RUN, server.getState());
    }

    @Test
    public void afterStopServerShouldHasIdleState() {
        final Server server = spy(new Server());
        doNothing().when(server).startServices();
        doNothing().when(server).stopServices();

        server.start();
        server.stop();

        assertEquals("Server after start should be in " + IDLE + " state", IDLE, server.getState());
    }

    @Test
    public void serverShouldExecuteAllTasksByScheduleTimeAndIncomeOrder() throws InterruptedException {
        final Server server = new Server();
        server.start();
        for (int count = 5; count > 0; count--) {
            final CountDownLatch latch = new CountDownLatch(4);
            final BlockingQueue<Integer> completed = new LinkedBlockingQueue<>();
            final LocalDateTime firstTask = LocalDateTime.now(ZoneId.of("UTC")); // 4
            final LocalDateTime secondTask = firstTask.plusSeconds(1); // 3
            final LocalDateTime thirdTask = secondTask.plusSeconds(1); // 2
            final LocalDateTime zeroTask = LocalDateTime.from(firstTask);// 1
            final List<TestExecutableTask> tasks = Stream.of(zeroTask, thirdTask, secondTask, firstTask)
                    .map(ldt -> new TestExecutableTask(latch, ldt, completed))
                    .collect(Collectors.toList());

            tasks.forEach(t -> server.scheduleTask(t.getScheduleTime(), t));
            latch.await(5, SECONDS);

            final Integer[] expected = tasks.stream().sorted().map(TestExecutableTask::getInnerNumber).toArray(Integer[]::new);
            final List<Integer> actual = new ArrayList<>();
            completed.drainTo(actual);
            assertEquals("All tasks should be executed", 0, latch.getCount());
            assertArrayEquals("Tasks should be executed in legal order", expected, actual.toArray(new Integer[actual.size()]));
        }
        server.stop();
    }

    @Test
    public void serverShouldAsyncExecuteAllTasksByScheduleTimeAndIncomeOrder() throws InterruptedException {
        int capacity = 30;
        final CountDownLatch awaitStart = new CountDownLatch(1);
        final CountDownLatch awaitComplete = new CountDownLatch(capacity);
        final BlockingQueue<Integer> completed = new LinkedBlockingQueue<>();
        final List<TestExecutableTask> tasks = new ArrayList<>(capacity);

        final Server server = new Server();
        server.start();
        final LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        // past
        executor.submit(() -> {
            awaitStart.countDown();
            final LocalDateTime pastTime = now.minusSeconds(2);
            for (int count = capacity / 3; count > 0; count--) {
                final TestExecutableTask task = new TestExecutableTask(awaitComplete, pastTime, completed);
                server.scheduleTask(pastTime, task);
                tasks.add(task);
            }
        });
        // current time
        executor.submit(() -> {
            awaitStart.countDown();
            for (int count = capacity / 3; count > 0; count--) {
                final TestExecutableTask task = new TestExecutableTask(awaitComplete, now, completed);
                server.scheduleTask(now, task);
                tasks.add(task);
            }
        });
        // future
        executor.submit(() -> {
            awaitStart.countDown();
            final LocalDateTime futureTime = now.plusSeconds(2);
            for (int count = capacity / 3; count > 0; count--) {
                final TestExecutableTask task = new TestExecutableTask(awaitComplete, futureTime, completed);
                server.scheduleTask(futureTime, task);
                tasks.add(task);
            }
        });
        awaitStart.await();
        awaitComplete.await(5, SECONDS);
        server.stop();

        final Integer[] expected = tasks.stream().sorted().map(TestExecutableTask::getInnerNumber).toArray(Integer[]::new);
        final List<Integer> actual = new ArrayList<>(capacity);
        completed.drainTo(actual);
        assertEquals("All tasks should be executed", 0, awaitComplete.getCount());
        assertArrayEquals("Tasks should be executed in legal order", expected, actual.toArray(new Integer[actual.size()]));
    }

    public static class TestExecutableTask implements Callable<Void>, Comparable<TestExecutableTask> {
        private static final Logger taskLogger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

        private static final Comparator<TestExecutableTask> comparator = buildComparator();
        private static final AtomicInteger innerIterator = new AtomicInteger(1);
        private final int innerNumber;
        private final CountDownLatch latch;
        private final LocalDateTime scheduleTime;
        private final BlockingQueue<Integer> queue;

        public TestExecutableTask(CountDownLatch latch, LocalDateTime scheduleTime, BlockingQueue<Integer> queue) {
            this.latch = latch;
            this.scheduleTime = scheduleTime;
            this.queue = queue;
            this.innerNumber = innerIterator.getAndIncrement();
        }

        @Override
        public Void call() throws Exception {
            queue.offer(innerNumber);
            latch.countDown();
            taskLogger.debug("Execute task; innerNumber: {}, scheduleTime: {}, now: {}",
                    innerNumber, scheduleTime, LocalDateTime.now(ZoneId.of("UTC")));
            return null;
        }


        public LocalDateTime getScheduleTime() {
            return scheduleTime;
        }

        public int getInnerNumber() {
            return innerNumber;
        }

        @Override
        public String toString() {
            return "TestExecutableTask{" +
                    "innerNumber=" + innerNumber +
                    ", scheduleTime=" + scheduleTime +
                    '}';
        }

        @Override
        public int compareTo(TestExecutableTask other) {
            return comparator.compare(this, checkNotNull(other, "TestExecutableTask is null"));
        }

        private static Comparator<TestExecutableTask> buildComparator() {
            return Comparator.comparing(TestExecutableTask::getScheduleTime)
                    .thenComparing(TestExecutableTask::getInnerNumber);
        }
    }
}
