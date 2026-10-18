package com.karton.delivery.commands;

import com.karton.delivery.gui.DeliveryGui;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.core.tools.picocli.CommandLine;

public final class DeliveryCommand {
    private static final LiteralArgumentBuilder<CommandSourceStack> deliveryCommand =
            Commands.literal("delivery")
                    .executes(DeliveryCommand::openDeliveryGui);


    public static void register() {
        CommandRegistrationCallback.EVENT.register(
                (
                        dispatcher,
                        buildContext,
                        selection) -> {
                    dispatcher.register(deliveryCommand);
                }
        );
    }

    private static int openDeliveryGui(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            DeliveryGui gui = new DeliveryGui(
                    player
            );

            gui.open();
        } catch (CommandSyntaxException e) {
            context.getSource().sendFailure(
                    Component.literal("This command should be called as player")
            );
        }

        return 1;
    }
}
