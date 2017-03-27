package ru.shishmakov.core;

import com.google.common.util.concurrent.MoreExecutors;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.util.Queues;

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
import static ru.shishmakov.core.LifeCycle.IDLE;
import static ru.shishmakov.core.LifeCycle.RUN;
import static ru.shishmakov.util.Threads.STOP_TIMEOUT_SEC;

/**
 * @author Dmitriy Shishmakov on 23.03.17
 */
public class ServerTest {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
            final List<ExecutableTask> tasks = buildExecutableTasks(latch, completed);

            tasks.forEach(t -> server.scheduleTask(t.getScheduleTime(), t));
            latch.await(5, SECONDS);

            final Integer[] expected = tasks.stream().sorted().map(ExecutableTask::getInnerNumber).toArray(Integer[]::new);
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
        final CountDownLatch latch = new CountDownLatch(capacity);
        final BlockingQueue<Integer> completed = new LinkedBlockingQueue<>();
        final List<ExecutableTask> tasks = new ArrayList<>(capacity);

        final Server server = new Server();
        server.start();
        final LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        // past
        executor.submit(() -> {
            final LocalDateTime pastTime = now.minusSeconds(2);
            for (int count = capacity / 3; count > 0; count--) {
                final ExecutableTask task = new ExecutableTask(latch, pastTime, completed);
                server.scheduleTask(pastTime, task);
                tasks.add(task);
            }
        });
        // current time
        executor.submit(() -> {
            for (int count = capacity / 3; count > 0; count--) {
                final ExecutableTask task = new ExecutableTask(latch, now, completed);
                server.scheduleTask(now, task);
                tasks.add(task);
            }
        });
        // future
        executor.submit(() -> {
            final LocalDateTime futureTime = now.plusSeconds(2);
            for (int count = capacity / 3; count > 0; count--) {
                final ExecutableTask task = new ExecutableTask(latch, futureTime, completed);
                server.scheduleTask(futureTime, task);
                tasks.add(task);
            }
        });
        latch.await(5, SECONDS);
        server.stop();

        final Integer[] expected = tasks.stream().sorted().map(ExecutableTask::getInnerNumber).toArray(Integer[]::new);
        final List<Integer> actual = new ArrayList<>(capacity);
        completed.drainTo(actual);
        assertEquals("All tasks should be executed", 0, latch.getCount());
        assertArrayEquals("Tasks should be executed in legal order", expected, actual.toArray(new Integer[actual.size()]));
    }

    private static List<ExecutableTask> buildExecutableTasks(CountDownLatch latch, BlockingQueue<Integer> completed) {
        final LocalDateTime firstTask = LocalDateTime.now(ZoneId.of("UTC"));
        final LocalDateTime secondTask = firstTask.plusSeconds(1);
        final LocalDateTime thirdTask = secondTask.plusSeconds(1);
        final LocalDateTime zeroTask = LocalDateTime.from(firstTask);
        return Stream.of(zeroTask, thirdTask, secondTask, firstTask)
                .map(ldt -> new ExecutableTask(latch, ldt, completed))
                .collect(Collectors.toList());
    }

    public static class ExecutableTask implements Callable<Void>, Comparable<ExecutableTask> {
        private static final Logger taskLogger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

        private static final Comparator<ExecutableTask> comparator = buildComparator();
        private static final AtomicInteger innerIterator = new AtomicInteger(1);
        private final int innerNumber;
        private final CountDownLatch latch;
        private final LocalDateTime scheduleTime;
        private final BlockingQueue<Integer> queue;

        public ExecutableTask(CountDownLatch latch, LocalDateTime scheduleTime, BlockingQueue<Integer> queue) {
            this.latch = latch;
            this.scheduleTime = scheduleTime;
            this.queue = queue;
            this.innerNumber = innerIterator.getAndIncrement();
        }

        @Override
        public Void call() throws Exception {
            Queues.offer(queue, innerNumber);
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
            return "ExecutableTask{" +
                    "innerNumber=" + innerNumber +
                    ", scheduleTime=" + scheduleTime +
                    '}';
        }

        @Override
        public int compareTo(ExecutableTask other) {
            return comparator.compare(this, checkNotNull(other, "ExecutableTask is null"));
        }

        private static Comparator<ServerTest.ExecutableTask> buildComparator() {
            return Comparator.comparing(ExecutableTask::getScheduleTime)
                    .thenComparing(ExecutableTask::getInnerNumber);
        }
    }
}
