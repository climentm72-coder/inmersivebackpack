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

public class InmersiveBackpackClient implements ClientModInitializer {
    public static KeyBinding openBackpackKey;

    @Override
    public void onInitializeClient() {
        HandledScreens.register(InmersiveBackpackMod.BACKPACK_SCREEN_HANDLER, SmartBackpackScreen::new);

        openBackpackKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.inmersivebackpack.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.inmersivebackpack.name"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openBackpackKey.wasPressed()) {
                if (client.currentScreen == null && client.player != null) {
                    ClientPlayNetworking.send(InmersiveBackpackMod.OPEN_BACKPACK_PACKET_ID, PacketByteBufs.create());
                }
            }
        });
    }
}
