package ru.shishmakov.core;

import org.junit.Test;

import static org.junit.Assert.fail;
import static ru.shishmakov.core.LifeCycle.IDLE;
import static ru.shishmakov.core.LifeCycle.RUN;

/**
 * @author Dmitriy Shishmakov on 23.03.17
 */
public class ServerTest {

    @Test
    public void afterStartServerShouldHasRunState() throws InterruptedException {
        final Server server = new Server();
        server.start();

        if (LifeCycle.isNotRun(server.getState())) fail("Server after start should be in " + RUN + " state");
    }

    @Test
    public void afterStopServerShouldHasIdleState() throws InterruptedException {
        final Server server = new Server();
        server.start();
        server.stop();

        if (LifeCycle.isNotIdle(server.getState())) fail("Server after start should be in " + IDLE + " state");
    }
}
