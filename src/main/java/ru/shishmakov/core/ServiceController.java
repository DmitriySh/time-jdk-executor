package ru.shishmakov.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.atomic.AtomicReference;

import static ru.shishmakov.core.LifeCycle.*;

/**
 * @author Dmitriy Shishmakov on 24.03.17
 */
public class ServiceController {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final AtomicReference<LifeCycle> SERVICES_STATE = new AtomicReference<>(IDLE);

    public void startServices() {
        logger.info("Services starting...");
        final LifeCycle state = SERVICES_STATE.get();
        if (LifeCycle.isNotIdle(state)) {
            logger.warn("Warning! services already started, state: {}", state);
            return;
        }

        try {
            SERVICES_STATE.set(INIT);
            // ???
        } catch (Throwable e) {
            logger.error("Exception occurred during starting node services", e);
        } finally {
            SERVICES_STATE.set(RUN);
            logger.info("Services started, state: {}", SERVICES_STATE.get());
        }
    }

    public void stopServices() {
        logger.info("Services stopping...");
        final LifeCycle state = SERVICES_STATE.get();
        if (LifeCycle.isNotRun(state)) {
            logger.warn("Warning! services already stopped, state: {}", state);
            return;
        }

        try {
            SERVICES_STATE.set(STOPPING);
            // ???
        } catch (Throwable e) {
            logger.error("Exception occurred during stopping node services", e);
        } finally {
            SERVICES_STATE.set(IDLE);
            logger.info("Services stopped, state: {}", SERVICES_STATE.get());
        }
    }
}
