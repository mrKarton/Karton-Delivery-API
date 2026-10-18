package com.karton.delivery.util;

import com.karton.delivery.ServerContextHolder;
import com.karton.delivery.database.DeliveryEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class PlayerInventoryUtil {
    public static boolean hasFreeSlots(ServerPlayer player, int requiredSlots) {
        if (requiredSlots <= 0) {
            return true;
        }

        int freeSlots = 0;

        for (int slot = 0; slot < 36; slot++) {
            if (player.getInventory().getItem(slot).isEmpty()) {
                if (++freeSlots >= requiredSlots) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean tryDeliverAtTheMoment(DeliveryEntry entry) {
        if (entry.id() != -1) {
            throw new IllegalArgumentException("This delivery is registered in database and should be delivered using it's methods");
        }

        ServerPlayer player = ServerContextHolder.getServerInstance()
                .getPlayerList()
                .getPlayer(entry.target());

        if (player == null) {
            try {
                UUID uuid = UUID.fromString(entry.target());
                player = ServerContextHolder.getServerInstance().getPlayerList().getPlayer(uuid);
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        }

        if (player == null) {
            return false;
        }

        if (!hasFreeSlots(player, 1)) {
            return false;
        }

        player.getInventory().add(entry.stack());
        player.sendSystemMessage(
                Component.translatable(
                        "message.karton-delivery.deliveries-applied"
                )
                        .withStyle(
                                ChatFormatting.GREEN
                        )
        );
        return true;
    }
}
