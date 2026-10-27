package net.climent.inmersivebackpack.client;

import net.climent.inmersivebackpack.InmersiveBackpackMod;
import net.climent.inmersivebackpack.screen.SmartBackpackScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Punto de entrada solo-cliente: asocia el tipo de menu con su pantalla y gestiona las teclas.
 */
public class InmersiveBackpackClient implements ClientModInitializer {

    public static KeyBinding openBackpackKey;

    @Override
    public void onInitializeClient() {
        // 1. Registrar la pantalla del menu
        HandledScreens.register(InmersiveBackpackMod.BACKPACK_SCREEN_HANDLER, SmartBackpackScreen::new);

        // 2. Registrar la tecla 'B' para abrir la mochila desde la espalda
        openBackpackKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.inmersivebackpack.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.inmersivebackpack.name"
        ));

        // 3. Escuchar las pulsaciones de la tecla en cada tick del cliente
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openBackpackKey.wasPressed()) {
                ClientPlayNetworking.send(InmersiveBackpackMod.OPEN_BACKPACK_PACKET_ID, PacketByteBufs.create());
            }
        });
    }
}
