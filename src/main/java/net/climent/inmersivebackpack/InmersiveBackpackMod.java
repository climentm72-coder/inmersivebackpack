package net.climent.inmersivebackpack;

import net.climent.inmersivebackpack.item.SmartBackpackItem;
import net.climent.inmersivebackpack.screen.BackpackScreenHandler;
import net.climent.inmersivebackpack.sleep.BedrollManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Punto de entrada comun (cliente + servidor).
 * Registra el item de la mochila y su tipo de ScreenHandler.
 */
public class InmersiveBackpackMod implements ModInitializer {

    public static final String MOD_ID = "inmersivebackpack";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Mochila: maxCount(1) -> no apilable.
     * Sin maxDamage -> sin durabilidad, nunca se desgasta.
     * fireproof() -> sobrevive a lava y fuego (no se quema como item en el suelo).
     * No existe ningun modificador de velocidad ni de peso en ninguna parte del mod.
     */
    public static final Item SMART_BACKPACK =
            new SmartBackpackItem(new Item.Settings().maxCount(1).fireproof());

    /** Se asigna en onInitialize (el registro de tipos de menu ocurre en el arranque). */
    public static ScreenHandlerType<BackpackScreenHandler> BACKPACK_SCREEN_HANDLER;

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        // 1. Item
        Registry.register(Registries.ITEM, id("smart_backpack"), SMART_BACKPACK);

        // 2. Tipo de menu. Usamos ExtendedScreenHandlerType (Fabric API) porque el servidor
        //    debe enviar datos extra al abrir el menu (el slot de la mochila bloqueada).
        //    ScreenHandlerRegistry esta obsoleto, por eso no se usa.
        BACKPACK_SCREEN_HANDLER = Registry.register(
                Registries.SCREEN_HANDLER,
                id("smart_backpack"),
                new ExtendedScreenHandlerType<BackpackScreenHandler>(BackpackScreenHandler::new));

        // 3. Pestana creativa (Herramientas)
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                .register(entries -> entries.add(SMART_BACKPACK));

        // 4. Sistema de la manta de campana
        BedrollManager.init();

        LOGGER.info("Inmersive Backpack cargado: Mochila Medieval de Viajero registrada.");
    }
}
