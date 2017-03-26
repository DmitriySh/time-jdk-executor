package ru.shishmakov.core;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.concurrent.Queues;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static ru.shishmakov.core.LifeCycle.IDLE;
import static ru.shishmakov.core.LifeCycle.RUN;

/**
 * @author Dmitriy Shishmakov on 23.03.17
 */
public class ServerTest {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
    public void serverShouldExecuteAllScheduleTasks() throws InterruptedException {
        final Server server = new Server();
        final CountDownLatch latch = new CountDownLatch(4);
        final BlockingQueue<Integer> completed = new LinkedBlockingQueue<>();
        final LocalDateTime firstTask = LocalDateTime.now(ZoneId.of("UTC"));
        final LocalDateTime secondTask = firstTask.plusSeconds(3);
        final LocalDateTime thirdTask = secondTask.plusSeconds(2);
        final LocalDateTime zeroTask = firstTask;

        server.start();
        server.scheduleTask(zeroTask, new ExecutableTask(latch, zeroTask, completed)); // 1
        server.scheduleTask(thirdTask, new ExecutableTask(latch, thirdTask, completed)); // 2
        server.scheduleTask(secondTask, new ExecutableTask(latch, secondTask, completed)); // 3
        server.scheduleTask(firstTask, new ExecutableTask(latch, firstTask, completed)); // 4

        try {
            latch.await(10, SECONDS);
        } catch (Exception e) {
            logger.error("Error in time of executing task!", e);
        } finally {
            server.stop();
        }

        assertEquals("All tasks should be executed", 0, latch.getCount());
    }

    public static class ExecutableTask implements Callable<Void> {
        private static final Logger taskLogger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

        private static final AtomicInteger innerIterator = new AtomicInteger(1);
        private final int innerNumber;
        private final CountDownLatch latch;
        private final LocalDateTime scheduleTime;
        private final BlockingQueue<Integer> completed;

        public ExecutableTask(CountDownLatch latch, LocalDateTime scheduleTime, BlockingQueue<Integer> completed) {
            this.latch = latch;
            this.scheduleTime = scheduleTime;
            this.completed = completed;
            this.innerNumber = innerIterator.getAndIncrement();
        }

        @Override
        public Void call() throws Exception {
            Queues.offer(completed, innerNumber);
            latch.countDown();
            taskLogger.debug("Execute task; innerNumber: {}, scheduleTime: {}, now: {}",
                    innerNumber, scheduleTime, LocalDateTime.now(ZoneId.of("UTC")));
            return null;
        }

        @Override
        public String toString() {
            return "ExecutableTask{" +
                    "scheduleTime=" + scheduleTime +
                    '}';
        }
    }
}
