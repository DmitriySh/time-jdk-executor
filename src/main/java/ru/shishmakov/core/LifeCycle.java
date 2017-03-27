package ru.shishmakov.core;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dmitriy Shishmakov on 12.03.17
 */
public enum LifeCycle {
    IDLE,
    INIT,
    RUN,
    STOPPING;

    public static boolean isNotIdle(LifeCycle state) {
        return !isIdle(state);
    }

    public static boolean isIdle(LifeCycle state) {
        return checkNotNull(state, "state is null") == IDLE;
    }

    public static boolean isNotStopping(LifeCycle state) {
        return !isStopping(state);
    }

    public static boolean isStopping(LifeCycle state) {
        return checkNotNull(state, "state is null") == STOPPING;
    }

    public static boolean isNotRun(LifeCycle state) {
        return !isRun(state);
    }

    public static boolean isRun(LifeCycle state) {
        return checkNotNull(state, "state is null") == RUN;
    }
}
