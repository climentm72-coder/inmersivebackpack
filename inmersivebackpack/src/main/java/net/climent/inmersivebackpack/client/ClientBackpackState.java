package net.climent.inmersivebackpack.client;

import net.climent.inmersivebackpack.network.WornInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Mochilas que llevan puestas los jugadores que ve este cliente (la rellena el servidor). */
public final class ClientBackpackState {

    private static final Map<UUID, WornInfo> WORN = new HashMap<>();

    private ClientBackpackState() {
    }

    public static void set(UUID player, WornInfo info) {
        if (info.variant() == 0) {
            WORN.remove(player);
        } else {
            WORN.put(player, info);
        }
    }

    public static WornInfo get(UUID player) {
        return WORN.getOrDefault(player, WornInfo.NONE);
    }

    public static void clear() {
        WORN.clear();
    }
}
