package net.climent.inmersivebackpack;

import net.climent.inmersivebackpack.item.SmartBackpackItem;
import net.climent.inmersivebackpack.network.NetworkIds;
import net.climent.inmersivebackpack.network.WornTracker;
import net.climent.inmersivebackpack.screen.BackpackScreenHandler;
import net.climent.inmersivebackpack.sleep.BedrollManager;
import net.climent.inmersivebackpack.util.BackpackUtil;
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
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Punto de entrada comun (cliente + servidor). */
public class InmersiveBackpackMod implements ModInitializer {

    public static final String MOD_ID = "inmersivebackpack";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** maxCount(1): no apilable. Sin maxDamage: sin durabilidad. fireproof: sobrevive a fuego y lava. */
    public static final Item SMART_BACKPACK =
            new SmartBackpackItem(new Item.Settings().maxCount(1).fireproof(), SmartBackpackItem.Variant.LEATHER);
    public static final Item SMART_BACKPACK_RABBIT =
            new SmartBackpackItem(new Item.Settings().maxCount(1).fireproof(), SmartBackpackItem.Variant.RABBIT);

    public static ScreenHandlerType<BackpackScreenHandler> BACKPACK_SCREEN_HANDLER;

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, id("smart_backpack"), SMART_BACKPACK);
        Registry.register(Registries.ITEM, id("smart_backpack_rabbit"), SMART_BACKPACK_RABBIT);

        BACKPACK_SCREEN_HANDLER = Registry.register(
                Registries.SCREEN_HANDLER,
                id("smart_backpack"),
                new ExtendedScreenHandlerType<BackpackScreenHandler>(BackpackScreenHandler::new));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(SMART_BACKPACK);
            entries.add(SMART_BACKPACK_RABBIT);
        });

        // Tecla B: abrir la mochila que llevas puesta
        ServerPlayNetworking.registerGlobalReceiver(NetworkIds.OPEN_BACKPACK, (server, player, handler, buf, sender) ->
                server.execute(() -> {
                    if (player.currentScreenHandler != player.playerScreenHandler || player.isSleeping()) {
                        return;
                    }
                    int index = BackpackUtil.findWornIndex(player);
                    if (index < 0) {
                        player.sendMessage(Text.translatable("message.inmersivebackpack.no_backpack"), true);
                        return;
                    }
                    ItemStack stack = player.getInventory().getStack(index);
                    player.openHandledScreen(new SmartBackpackItem.BackpackFactory(stack, index));
                }));

        BedrollManager.init();
        WornTracker.init();

        LOGGER.info("Inmersive Backpack cargado.");
    }
}
