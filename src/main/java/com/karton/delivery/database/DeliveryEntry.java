package com.karton.delivery.database;

import com.karton.delivery.ServerContextHolder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;

public record DeliveryEntry(ItemStack stack, String sender, String target, int id) {
    public String serializeItem() {
        CompoundTag tag = (CompoundTag) ItemStack.CODEC.encodeStart(
                ServerContextHolder.getRegistries().createSerializationContext(NbtOps.INSTANCE),
                this.stack
        ).getOrThrow();

        return tag.toString();
    }

    public DeliveryEntry(ItemStack stack, String sender, String target) {
        this(
                stack,
                sender,
                target,
                -1
        );
    }

    public DeliveryEntry withId(int id) {
        return new DeliveryEntry(
                stack(),
                sender(),
                target(),
                id
        );
    }

    public static DeliveryEntry fromRaw (
            String srializedItemStack,
            String sender,
            String target,
            int id
    ) throws CommandSyntaxException {
        CompoundTag tag = TagParser.parseCompoundFully(srializedItemStack);

        ItemStack item = ItemStack.CODEC.parse(
                ServerContextHolder.getRegistries().createSerializationContext(NbtOps.INSTANCE),
                tag
        ).getOrThrow();

        return new DeliveryEntry(
                item,
                sender,
                target,
                id
        );
    }
}
