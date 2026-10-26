package net.climent.inmersivebackpack.network;

/**
 * Resumen de la mochila que lleva un jugador.
 *
 * @param variant 0 = ninguna, 1 = cuero de vaca, 2 = piel de conejo
 * @param lantern 0 = sin farol, 1 = farol, 2 = farol de almas
 * @param bedroll true si lleva una manta equipada
 */
public record WornInfo(int variant, int lantern, boolean bedroll) {
    public static final WornInfo NONE = new WornInfo(0, 0, false);
}
