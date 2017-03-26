package ru.shishmakov.core;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author Dmitriy Shishmakov on 26.03.17
 */
public class TimeTaskTest {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Test
    public void timeTasksShouldSortByScheduleTimeAndIncomeOrder() {
        int order = 0;
        final LocalDateTime firstTask = LocalDateTime.now(ZoneId.of("UTC"));
        final LocalDateTime secondTask = firstTask.plusSeconds(3);
        final LocalDateTime thirdTask = secondTask.plusSeconds(2);
        final LocalDateTime zeroTask = firstTask;

        final List<TimeTask> list = new ArrayList<>();
        list.add(new TimeTask(++order, zeroTask, null)); // 1
        list.add(new TimeTask(++order, thirdTask, null)); // 2
        list.add(new TimeTask(++order, secondTask, null)); // 3
        list.add(new TimeTask(++order, firstTask, null)); // 4

        Collections.sort(list);

        final Integer[] expected = {1, 4, 3, 2};
        final Integer[] actual = list.stream().map(TimeTask::getOrderId).toArray(Integer[]::new);
        assertArrayEquals("Time tasks should be sorted", expected, actual);
    }
}
