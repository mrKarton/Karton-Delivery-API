package com.karton.delivery.util;

import com.karton.delivery.KartonDeliveryAPI;
import com.karton.delivery.ServerContextHolder;
import com.karton.delivery.api.Deliveries;
import com.karton.delivery.database.DeliveriesDatabase;
import com.karton.delivery.database.DeliveryEntry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class OnPlayerJoin {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> {
                    var player = handler.player;

                    if (player instanceof ServerPlayer serverPlayer) {
                        playerJoinAction(serverPlayer);
                    }
                }
        );
    }

    private static void playerJoinAction(ServerPlayer player) {
        List<DeliveryEntry> deliveries = Deliveries.getDeliveriesFor(player);
        if (deliveries.isEmpty()) {
            return;
        }
        if (PlayerInventoryUtil.hasFreeSlots(player, deliveries.size())) {
            for (DeliveryEntry entry : deliveries) {
                DeliveriesDatabase.deleteDeliveryAsync(entry.id())
                        .whenComplete(
                                (result, error) -> {
                                    if (error != null) {
                                        KartonDeliveryAPI.LOGGER.error(error.getMessage());
                                        return;
                                    }

                                    ServerContextHolder.executeInServerThread(
                                            () -> {
                                                player.getInventory().add(entry.stack());
                                            }
                                    );
                                }
                        );
            }

            player.sendSystemMessage(
                    Component.translatable(
                            "message.karton-delivery.deliveries-applied"
                    )
                            .withStyle(
                                    ChatFormatting.GREEN
                            )
            );

            return;
        }

        player.sendSystemMessage(
                Component.translatable(
                        "message.karton-delivery.deliveries-unapplied"
                )
                        .withStyle(
                                ChatFormatting.YELLOW
                        )
        );
    }
}
