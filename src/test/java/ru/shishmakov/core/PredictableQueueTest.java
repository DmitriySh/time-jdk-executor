package ru.shishmakov.core;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.util.PredictableQueue;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Dmitriy Shishmakov on 26.03.17
 */
public class PredictableQueueTest {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Test
    public void pollAndDrainToShouldNotGetItemsIfPredicateProhibitRetrieves() {
        final PredictableQueue<Integer> queue = new PredictableQueue<>(1, e -> false);
        queue.addAll(Arrays.asList(1, 3, 2, 0));
        assertFalse("Queue should not be empty", queue.isEmpty());

        final List<Integer> temp = new ArrayList<>();
        for (int count = 5; count > 0; count--) {
            queue.poll().ifPresent(temp::add);
            queue.drainTo(temp, queue.size());
        }
        assertTrue("Numbers should be sorted", temp.isEmpty());
    }

    @Test
    public void addAllShouldFillQueueAndDrainToShouldGetNumbersSortedByNaturalOrder() {
        final PredictableQueue<Integer> queue = new PredictableQueue<>(1);
        queue.addAll(Arrays.asList(1, 3, 2, 0));
        assertFalse("Queue should not be empty", queue.isEmpty());

        final List<Integer> temp = new ArrayList<>(queue.size());
        queue.drainTo(temp);

        final Integer[] expected = {0, 1, 2, 3};
        final Integer[] actual = temp.stream().toArray(Integer[]::new);
        assertArrayEquals("Numbers should be sorted", expected, actual);
    }

    @Test
    public void offerShouldFillQueueAndPollShouldGetTimeTasksSortedByScheduleTimeAndIncomeOrder() {
        int order = 0;
        final LocalDateTime firstTask = LocalDateTime.now(ZoneId.of("UTC"));
        final LocalDateTime secondTask = firstTask.plusSeconds(3);
        final LocalDateTime thirdTask = secondTask.plusSeconds(2);
        final LocalDateTime zeroTask = LocalDateTime.from(firstTask);

        final PredictableQueue<TimeTask> queue = new PredictableQueue<>();
        queue.offer(new TimeTask(++order, zeroTask, null)); // 1
        queue.offer(new TimeTask(++order, thirdTask, null)); // 2
        queue.offer(new TimeTask(++order, secondTask, null)); // 3
        queue.offer(new TimeTask(++order, firstTask, null)); // 4
        assertFalse("Queue should not be empty", queue.isEmpty());

        final List<TimeTask> temp = new ArrayList<>(queue.size());
        while (!queue.isEmpty()) queue.poll().ifPresent(temp::add);

        final Integer[] expected = {1, 4, 3, 2};
        final Integer[] actual = temp.stream().map(TimeTask::getOrderId).toArray(Integer[]::new);
        assertArrayEquals("Time tasks should be sorted", expected, actual);
    }
}
