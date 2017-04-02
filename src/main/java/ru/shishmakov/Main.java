package ru.shishmakov;

import ru.shishmakov.core.Server;

/**
 * @author Dmitriy Shishmakov on 23.03.17
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        new Server()
                .startAsync()
                .await();
    }
}
