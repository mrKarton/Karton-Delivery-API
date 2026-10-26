package com.karton.delivery.api;

import com.karton.delivery.database.DeliveriesDatabase;
import com.karton.delivery.database.DeliveryEntry;
import com.karton.delivery.util.PlayerInventoryUtil;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
            DeliveriesDatabase.sendAndCahceAsync(entry);
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

    public static void sendBatch(ArrayList<ItemStack> items, String target, String sender) {
        ArrayList<DeliveryEntry> entires = items
                .stream()
                .map(
                        stack ->  new DeliveryEntry(
                                stack,
                                sender,
                                target
                        )
                )
                .collect(
                        Collectors.toCollection(ArrayList::new)
                );


    }

    public static void sendBatch(ArrayList<ItemStack> stack, ServerPlayer target, String sender) {
        sendBatch(stack, target.getStringUUID(), sender);
    }

    public static void sendBatch(ArrayList<ItemStack> stack, ServerPlayer target) {
        sendBatch(stack, target, "SYSTEM");
    }

    public static void sendBatch(ArrayList<ItemStack> stacks, String target) {
        sendBatch(stacks, target, "SYSTEM");
    }
}
