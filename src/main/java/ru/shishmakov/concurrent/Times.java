package ru.shishmakov.concurrent;

/**
 * @author Dmitriy Shishmakov on 26.03.17
 */
public class Times {

    public static boolean isTimeExpired(long time) {
        return time <= System.currentTimeMillis();
    }

    public static boolean isTimeNotExpired(long time) {
        return !isTimeExpired(time);
    }
}
