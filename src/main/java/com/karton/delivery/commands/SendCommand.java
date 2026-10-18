package com.karton.delivery.commands;

import com.karton.delivery.api.Deliveries;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;


public final class SendCommand {
    private static int sendItemCommand (CommandContext<CommandSourceStack> context) {
        String player = StringArgumentType.getString(
                context,
                "playerNameOrUuid"
        );
        Item item = ItemArgument.getItem(
                context,
                "item"
        ).item().value();

        String sender = context.getSource().getTextName();

        return sendItem(player, item, 1, sender);
    }

    private static int sendItemCommandWithCount (CommandContext<CommandSourceStack> context) {
        String player = StringArgumentType.getString(
                context,
                "playerNameOrUuid"
        );
        Item item = ItemArgument.getItem(
                context,
                "item"
        ).item().value();
        int count = IntegerArgumentType.getInteger(
                context,
                "count"
        );

        String sender = context.getSource().getTextName();
        context.getSource().sendSuccess(
                () -> Component.translatable(
                        "message.karton-delivery.delivery-sent",
                            player
                    )
                            .withStyle(ChatFormatting.GREEN),
                false
        );
        return sendItem(player, item, count, sender);
    }

    private static int sendItem(String player, Item item, int count, String sender) {
        ItemStack stack = new ItemStack(item);
        stack.setCount(count);

        Deliveries.sendItem(stack, player, sender);
        return 1;
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, buildContext, selection) -> {
                    dispatcher.register(
                            Commands.literal("senditem")
                                    .requires(
                                            Permissions.require(
                                                    "karton.delivery.send",
                                                    PermissionLevel.OWNERS
                                            )
                                    )
                                    .then(
                                            Commands.argument(
                                                            "playerNameOrUuid",
                                                            StringArgumentType.word()
                                                    )
                                                    .then(
                                                            Commands.argument(
                                                                            "item",
                                                                            ItemArgument.item(buildContext)
                                                                    )
                                                                    .executes(SendCommand::sendItemCommand)
                                                                    .then(
                                                                            Commands.argument(
                                                                                            "count",
                                                                                            IntegerArgumentType.integer(1)
                                                                                    )
                                                                                    .executes(SendCommand::sendItemCommandWithCount)
                                                                    )
                                                    )
                                    )
                    );
                }
        );
    }
}
