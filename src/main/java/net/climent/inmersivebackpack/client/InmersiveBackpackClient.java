package net.climent.inmersivebackpack.client;

import net.climent.inmersivebackpack.InmersiveBackpackMod;
import net.climent.inmersivebackpack.screen.SmartBackpackScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

/**
 * Punto de entrada solo-cliente: asocia el tipo de menu con su pantalla.
 */
public class InmersiveBackpackClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HandledScreens.register(InmersiveBackpackMod.BACKPACK_SCREEN_HANDLER, SmartBackpackScreen::new);
    }
}
