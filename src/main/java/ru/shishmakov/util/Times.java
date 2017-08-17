package ru.shishmakov.util;

/**
 * @author Dmitriy Shishmakov on 17.08.17
 */
public class Times {

    public static boolean isTimeExpired(long time) {
        return time <= System.currentTimeMillis();
    }

    public static boolean isTimeNotExpired(long time) {
        return !isTimeExpired(time);
    }
}
