package com.karton.delivery;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;

public final class ServerContextHolder {
    private static volatile MinecraftServer server;

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(
                startedServer -> server = startedServer
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(
                stoppedServer -> {
                    if (server == stoppedServer) {
                        server = null;
                    }
                }
        );
    }

    public static HolderLookup.Provider getRegistries() {
        MinecraftServer current = server;

        if (current == null) {
            throw new IllegalStateException("Server not loaded");
        }

        return current.registryAccess();
    }

    public static void executeInServerThread(Runnable action) {
        server.execute(action);
    }

    public static MinecraftServer getServerInstance() {
        return server;
    }
}
