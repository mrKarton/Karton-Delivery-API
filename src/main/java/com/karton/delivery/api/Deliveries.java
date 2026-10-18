package com.karton.delivery.api;

import com.karton.delivery.database.DeliveriesDatabase;
import com.karton.delivery.database.DeliveryEntry;
import com.karton.delivery.util.PlayerInventoryUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class Deliveries {
    public static List<DeliveryEntry> getDeliveriesFor(ServerPlayer player) {
        return DeliveriesDatabase.getCachedDeliveries()
                .stream()
                .filter(
                        entry ->  player.getGameProfile().name().equals(entry.target())
                            ||  player.getStringUUID().equals(entry.target())
                )
                .toList();
    }

    public static void sendItem(ItemStack stack, String target, String sender) {
        DeliveryEntry entry = new DeliveryEntry(
                stack,
                sender,
                target
        );

        if (!PlayerInventoryUtil.tryDeliverAtTheMoment(entry)) {
            DeliveriesDatabase.sendAndCahceSync(entry);
        }
    }

    public static void sendItem(ItemStack stack, ServerPlayer target, String sender) {
        sendItem(stack, target.getStringUUID(), sender);
    }

    public static void sendItem(ItemStack stack, ServerPlayer target) {
        sendItem(stack, target, "SYSTEM");
    }

    public static void sendItem(ItemStack stack, String target) {
        sendItem(stack, target, "SYSTEM");
    }
}
