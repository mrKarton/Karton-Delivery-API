package com.karton.delivery.gui;

import com.karton.delivery.KartonDeliveryAPI;
import com.karton.delivery.ServerContextHolder;
import com.karton.delivery.api.Deliveries;
import com.karton.delivery.database.DeliveriesDatabase;
import com.karton.delivery.database.DeliveryEntry;
import com.karton.fancygui.gui.ListViewScreen;
import com.karton.fancygui.util.NumberRange;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DeliveryGui {
    private final ServerPlayer player;
    private final ListViewScreen gui;
    private List<DeliveryEntry> deliveries;

    public DeliveryGui(
            ServerPlayer player
    ) {
        this.player = player;
        gui = new ListViewScreen(
                player,
                "Deliveries",
                List.of(),
                new NumberRange[0]
        );

        gui.setTitle(
                Component.translatable("text.karton-delivery.title.delivery-gui-title")
        );

        gui.setItemClickCallback(this::onItemClick);

        refresh();
    }

    public void open() {
        gui.open();
    }

    private void refresh() {
        List<DeliveryEntry> deliveries = Deliveries.getDeliveriesFor(player);
        this.deliveries = deliveries;

        List<ItemStack> displayStacks = new ArrayList<>();

        for (DeliveryEntry entry : deliveries) {
            ItemStack stack = entry.stack().copy();

            ItemLore lore = stack.getOrDefault(
                    DataComponents.LORE,
                    ItemLore.EMPTY
            );

            lore = lore.withLineAdded(
                    Component.translatable(
                            "text.karton-delivery.from",
                            entry.sender()
                    )
            );

            stack.set(DataComponents.LORE, lore);

            displayStacks.add(stack);
        }

        gui.setItems(displayStacks);
    }

    private void onItemClick(int index) {
        DeliveryEntry entry = deliveries.get(index);

        DeliveriesDatabase.deleteDeliveryAndCacheSync(entry.id())
                .whenComplete(
                        (result, error) -> {
                            if (error != null) {
                                KartonDeliveryAPI.LOGGER.error(error.getMessage());
                                return;
                            }

                            ServerContextHolder.executeInServerThread(
                                    () -> {
                                        refresh();
                                        gui.setCarried(entry.stack());
                                    }
                            );
                        }
                );
    }
}
