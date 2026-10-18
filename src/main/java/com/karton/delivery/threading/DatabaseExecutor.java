package com.karton.delivery.threading;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DatabaseExecutor {
    private static final ExecutorService EXECUTOR =
            Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "Karton-Delivery/DB");
                thread.setDaemon(true);
                return thread;
            });

    public static ExecutorService executor() {
        return EXECUTOR;
    }

    public static void shutdown() {
        EXECUTOR.shutdown();
    }

    private DatabaseExecutor() {}
}