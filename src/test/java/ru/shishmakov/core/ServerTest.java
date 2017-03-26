package ru.shishmakov.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static ru.shishmakov.core.LifeCycle.IDLE;
import static ru.shishmakov.core.LifeCycle.RUN;

/**
 * @author Dmitriy Shishmakov on 23.03.17
 */
public class ServerTest {

    @Test
    public void afterStartServerShouldHasRunState() throws InterruptedException {
        final Server server = spy(new Server());
        doNothing().when(server).startServices();
        doNothing().when(server).stopServices();

        server.start();

        assertEquals("Server after start should be in " + RUN + " state", RUN, server.getState());
    }

    @Test
    public void afterStopServerShouldHasIdleState() throws InterruptedException {
        final Server server = spy(new Server());
        doNothing().when(server).startServices();
        doNothing().when(server).stopServices();

        server.start();
        server.stop();

        assertEquals("Server after start should be in " + IDLE + " state", IDLE, server.getState());
    }
}
