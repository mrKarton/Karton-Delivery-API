package com.karton.delivery;

import com.karton.delivery.commands.DeliveryCommand;
import com.karton.delivery.commands.SendCommand;
import com.karton.delivery.config.ConfigManager;
import com.karton.delivery.database.ConnectionManager;
import com.karton.delivery.database.DeliveriesDatabase;
import com.karton.delivery.database.DeliveryEntry;
import com.karton.delivery.util.OnPlayerJoin;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;

import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;

public class KartonDeliveryAPI implements ModInitializer {
	public static final String MOD_ID = "karton-delivery-api";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ServerContextHolder.init();
		ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStart);
		ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);
		OnPlayerJoin.register();

		SendCommand.register();
		DeliveryCommand.register();
	}

	private void onServerStart(MinecraftServer server) {
        try {
            ConfigManager.init();
			ConnectionManager.createConnection(ConfigManager.getMainConfig().getJdbcConnectionString());
			DeliveriesDatabase.initTables();
			DeliveriesDatabase.loadDeliveriesToCache();
        } catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

	private void onServerStop(MinecraftServer server) {
		ConnectionManager.closeConnection();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
