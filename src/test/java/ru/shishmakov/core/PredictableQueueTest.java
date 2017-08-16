package ru.shishmakov.core;

import org.junit.Test;
import ru.shishmakov.BaseTest;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Dmitriy Shishmakov on 26.03.17
 */
public class PredictableQueueTest extends BaseTest {

    @Test
    public void drainToShouldGetFirstItemsLessTwoIfPredicateAssignRuleRetrieveLessTwo() {
        final PredictableQueue<Integer> queue = new PredictableQueue<>(1, e -> e < 2);
        queue.addAll(Arrays.asList(1, 1, 1, 2, 3, 4));
        assertFalse("Queue should not be empty", queue.isEmpty());

        final List<Integer> temp = new ArrayList<>();
        queue.drainTo(temp, queue.size());

        final Integer[] actual = temp.toArray(new Integer[0]);
        assertArrayEquals("Numbers should be sorted", new Integer[]{1, 1, 1}, actual);
    }

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

        final Integer[] actual = temp.toArray(new Integer[0]);
        assertArrayEquals("Numbers should be sorted", new Integer[]{0, 1, 2, 3}, actual);
    }

    @Test
    public void offerShouldFillQueueAndPollShouldGetTimeTasksSortedByScheduleTimeAndIncomeOrder() {
        int order = 0;
        final LocalDateTime firstTask = LocalDateTime.now(ZoneId.of("UTC"));
        final LocalDateTime secondTask = firstTask.plusSeconds(3);
        final LocalDateTime thirdTask = secondTask.plusSeconds(2);
        final LocalDateTime zeroTask = LocalDateTime.from(firstTask);

        final PredictableQueue<TimeTask> queue = new PredictableQueue<>();
        queue.offer(new TimeTask(zeroTask, null)); // 1
        queue.offer(new TimeTask(thirdTask, null)); // 2
        queue.offer(new TimeTask(secondTask, null)); // 3
        queue.offer(new TimeTask(firstTask, null)); // 4
        assertFalse("Queue should not be empty", queue.isEmpty());

        final List<TimeTask> temp = new ArrayList<>(queue.size());
        while (!queue.isEmpty()) queue.poll().ifPresent(temp::add);

        final Integer[] actual = temp.stream().map(TimeTask::getOrderId).toArray(Integer[]::new);
        assertArrayEquals("Time tasks should be sorted", new Integer[]{1, 4, 3, 2}, actual);
    }
}
