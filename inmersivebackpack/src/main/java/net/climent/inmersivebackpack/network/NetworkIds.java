package net.climent.inmersivebackpack.network;

import net.climent.inmersivebackpack.InmersiveBackpackMod;
import net.minecraft.util.Identifier;

/** Identificadores de los paquetes de red del mod. */
public final class NetworkIds {
    /** Cliente -> servidor: "abre la mochila que llevo puesta" (tecla B). */
    public static final Identifier OPEN_BACKPACK = InmersiveBackpackMod.id("open_backpack");
    /** Servidor -> clientes: que mochila lleva puesta un jugador (para dibujarla en su espalda). */
    public static final Identifier WORN_INFO = InmersiveBackpackMod.id("worn_info");

    private NetworkIds() {
    }
}
