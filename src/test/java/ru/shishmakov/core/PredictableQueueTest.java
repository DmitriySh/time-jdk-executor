package ru.shishmakov.core;

import org.junit.Test;
import ru.shishmakov.BaseTest;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.*;

/**
 * @author Dmitriy Shishmakov on 26.03.17
 */
public class PredictableQueueTest extends BaseTest {

    @Test
    public void drainToShouldGetFirstItemsLessTwoIfPredicateRuleRetrieveLessTwo() {
        Predicate<Integer> lessTwo = e -> e < 2;
        final PredictableQueue<Integer> queue = new PredictableQueue<>(1, lessTwo);
        queue.addAll(Arrays.asList(1, 2, 3, 1, 0, 4));
        int sizeBefore = queue.size();
        assertFalse("Queue should not be empty", queue.isEmpty());

        final List<Integer> bag = new ArrayList<>();
        queue.drainTo(bag, queue.size());

        final Integer[] actual = bag.toArray(new Integer[0]);
        assertEquals("Queue size should be less", sizeBefore - bag.size(), queue.size());
        assertArrayEquals("Numbers should be less 2", new Integer[]{0, 1, 1}, actual);
    }

    @Test
    public void pollAndDrainToShouldNotGetItemsIfPredicateProhibitAnyRetrievals() {
        Predicate<Integer> prohibitRetrievals = e -> false;
        final PredictableQueue<Integer> queue = new PredictableQueue<>(1, prohibitRetrievals);
        queue.addAll(Arrays.asList(1, 3, 2, 0));
        int sizeBefore = queue.size();
        assertFalse("Queue should not be empty", queue.isEmpty());

        final List<Integer> bag = new ArrayList<>();
        for (int effort = 5; effort > 0; effort--) {
            queue.poll().ifPresent(bag::add);
            queue.drainTo(bag, queue.size());
        }
        assertTrue("Numbers did not extract", bag.isEmpty());
        assertEquals("Queue size should be the same", sizeBefore, queue.size());
    }

    @Test
    public void addAllShouldFillQueueAndDrainToShouldGetNumbersSortedByNaturalOrder() {
        Predicate<Integer> retrieveAll = e -> true;
        final PredictableQueue<Integer> queue = new PredictableQueue<>(1, retrieveAll);
        queue.addAll(Arrays.asList(1, 3, 2, 0));
        assertFalse("Queue should not be empty", queue.isEmpty());

        final List<Integer> bag = new ArrayList<>(queue.size());
        queue.drainTo(bag);

        final Integer[] actual = bag.toArray(new Integer[0]);
        assertArrayEquals("Numbers should be sorted", new Integer[]{0, 1, 2, 3}, actual);
        assertTrue("Queue should be empty", queue.isEmpty());
    }

    @Test
    public void offerShouldFillQueueAndPollShouldGetTimeTasksSortedByScheduleTimeAndIncomeOrder() {
        final LocalDateTime firstTask = LocalDateTime.now(ZoneId.of("UTC"));
        final LocalDateTime secondTask = firstTask.plusSeconds(3);
        final LocalDateTime thirdTask = secondTask.plusSeconds(2);
        final LocalDateTime zeroTask = LocalDateTime.from(firstTask);

        final PredictableQueue<TimeTask> queue = new PredictableQueue<>(e -> true);
        queue.offer(new TimeTask(zeroTask, null)); // 1 order
        queue.offer(new TimeTask(thirdTask, null)); // 2 order
        queue.offer(new TimeTask(secondTask, null)); // 3 order
        queue.offer(new TimeTask(firstTask, null)); // 4 order
        assertFalse("Queue should not be empty", queue.isEmpty());

        final List<TimeTask> bag = new ArrayList<>(queue.size());
        while (!queue.isEmpty()) queue.poll().ifPresent(bag::add);

        final Integer[] actual = bag.stream().map(TimeTask::getOrderId).toArray(Integer[]::new);
        assertArrayEquals("Time tasks should be sorted by datetime and order", new Integer[]{1, 4, 3, 2}, actual);
    }
}
