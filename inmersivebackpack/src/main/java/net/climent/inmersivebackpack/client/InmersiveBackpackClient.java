package net.climent.inmersivebackpack.client;

import net.climent.inmersivebackpack.InmersiveBackpackMod;
import net.climent.inmersivebackpack.network.NetworkIds;
import net.climent.inmersivebackpack.network.WornInfo;
import net.climent.inmersivebackpack.screen.SmartBackpackScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EntityType;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

/** Punto de entrada solo-cliente. */
public class InmersiveBackpackClient implements ClientModInitializer {

    public static KeyBinding openKey;
    public static KeyBinding guideKey;

    private boolean wasSleeping = false;
    private Perspective savedPerspective = Perspective.FIRST_PERSON;
    /** Ticks que faltan para devolver la camara a como estaba (-1 = nada pendiente). */
    private int restoreDelay = -1;

    @Override
    public void onInitializeClient() {
        HandledScreens.register(InmersiveBackpackMod.BACKPACK_SCREEN_HANDLER, SmartBackpackScreen::new);

        // Modelo 3D de la mochila en la espalda
        EntityModelLayerRegistry.registerModelLayer(BackpackModel.LAYER, BackpackModel::getTexturedModelData);
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityType == EntityType.PLAYER && entityRenderer instanceof PlayerEntityRenderer playerRenderer) {
                registrationHelper.register(new BackpackFeatureRenderer(playerRenderer, context.getModelLoader()));
            }
        });

        // Que mochila lleva cada jugador (la envia el servidor)
        ClientPlayNetworking.registerGlobalReceiver(NetworkIds.WORN_INFO, (client, handler, buf, sender) -> {
            UUID player = buf.readUuid();
            int variant = buf.readByte();
            int lantern = buf.readByte();
            boolean bedroll = buf.readBoolean();
            client.execute(() -> ClientBackpackState.set(player, new WornInfo(variant, lantern, bedroll)));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientBackpackState.clear());

        // Teclas
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.inmersivebackpack.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.inmersivebackpack"));
        guideKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.inmersivebackpack.guide", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.inmersivebackpack"));

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        while (openKey.wasPressed()) {
            if (client.currentScreen == null && client.player != null) {
                ClientPlayNetworking.send(NetworkIds.OPEN_BACKPACK, PacketByteBufs.create());
            }
        }
        while (guideKey.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new GuideScreen(null));
            }
        }

        // Cinematica: al acostarse la camara pasa a tercera persona y, al levantarse,
        // vuelve a como estaba tras un instante para verte ponerte de pie.
        boolean sleeping = client.player != null && client.player.isSleeping();
        if (sleeping && !wasSleeping) {
            if (restoreDelay < 0) {
                savedPerspective = client.options.getPerspective();
            }
            restoreDelay = -1;
            client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        } else if (!sleeping && wasSleeping) {
            restoreDelay = 24;
        }
        wasSleeping = sleeping;

        if (restoreDelay > 0) {
            restoreDelay--;
        } else if (restoreDelay == 0) {
            client.options.setPerspective(savedPerspective);
            restoreDelay = -1;
        }
    }
}
