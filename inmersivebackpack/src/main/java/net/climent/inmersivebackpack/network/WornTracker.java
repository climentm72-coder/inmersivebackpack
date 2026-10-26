package net.climent.inmersivebackpack.network;

import net.climent.inmersivebackpack.light.LanternLight;
import net.climent.inmersivebackpack.util.BackpackUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cada 4 ticks mira que mochila lleva cada jugador, actualiza la luz del farol y avisa
 * a los clientes que lo ven para que le dibujen la mochila en la espalda.
 */
public final class WornTracker {

    private static final Map<UUID, WornInfo> LAST = new HashMap<>();

    private WornTracker() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int tick = server.getTicks();
            if (tick % 4 != 0) {
                return;
            }
            boolean resend = tick % 60 == 0; // reenvio periodico para quien empieza a ver al jugador despues
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                WornInfo info = BackpackUtil.info(player);
                LanternLight.update(player, info.lantern());
                WornInfo last = LAST.get(player.getUuid());
                if (resend || !info.equals(last)) {
                    LAST.put(player.getUuid(), info);
                    broadcast(player, info);
                }
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.getPlayer().getUuid();
            LAST.remove(id);
            LanternLight.clear(server, id);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(LanternLight::clearAll);
    }

    private static void broadcast(ServerPlayerEntity player, WornInfo info) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(player.getUuid());
        buf.writeByte(info.variant());
        buf.writeByte(info.lantern());
        buf.writeBoolean(info.bedroll());

        for (ServerPlayerEntity viewer : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(viewer, NetworkIds.WORN_INFO, PacketByteBufs.copy(buf));
        }
        ServerPlayNetworking.send(player, NetworkIds.WORN_INFO, PacketByteBufs.copy(buf));
    }
}
