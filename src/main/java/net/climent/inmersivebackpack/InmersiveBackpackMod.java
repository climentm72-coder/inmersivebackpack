package net.climent.inmersivebackpack;

import net.climent.inmersivebackpack.item.SmartBackpackItem;
import net.climent.inmersivebackpack.screen.BackpackScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InmersiveBackpackMod implements ModInitializer {
    public static final String MOD_ID = "inmersivebackpack";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Item SMART_BACKPACK_ITEM = Registry.register(
        Registries.ITEM,
        new Identifier(MOD_ID, "smart_backpack"),
        new SmartBackpackItem(new Item.Settings().maxCount(1))
    );

    public static ScreenHandlerType<BackpackScreenHandler> BACKPACK_SCREEN_HANDLER;
    public static final Identifier OPEN_BACKPACK_PACKET = new Identifier(MOD_ID, "open_backpack");

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Medieval Traveler's Backpack");

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).accept(entries -> entries.add(SMART_BACKPACK_ITEM));

        BACKPACK_SCREEN_HANDLER = ExtendedScreenHandlerType.register(
            new Identifier(MOD_ID, "backpack"),
            BackpackScreenHandler::new
        );

        ServerPlayNetworking.registerGlobalReceiver(OPEN_BACKPACK_PACKET, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                ItemStack stack = player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST);
                if (stack.getItem() instanceof SmartBackpackItem) {
                    int lockedIndex = -1;
                    player.openHandledScreen(new net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory() {
                        @Override
                        public void writeScreenOpeningData(net.minecraft.server.network.ServerPlayerEntity player, net.minecraft.network.PacketByteBuf buf) {
                            buf.writeInt(lockedIndex);
                        }
                        @Override
                        public net.minecraft.text.Text getDisplayName() {
                            return stack.getName();
                        }
                        @Override
                        public net.minecraft.screen.ScreenHandler createMenu(int syncId, net.minecraft.entity.player.PlayerInventory playerInventory, net.minecraft.entity.player.PlayerEntity player) {
                            return new BackpackScreenHandler(syncId, playerInventory, new net.climent.inmersivebackpack.inventory.BackpackInventory(stack), stack, lockedIndex);
                        }
                    });
                }
            });
        });
    }
}
