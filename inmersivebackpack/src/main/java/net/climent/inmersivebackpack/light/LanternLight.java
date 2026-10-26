package net.climent.inmersivebackpack.light;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightBlock;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Luz dinamica del farol de la mochila: mientras el jugador lleva un farol en la casilla azul,
 * se coloca un bloque de luz invisible (minecraft:light) a la altura de su cabeza y se va moviendo
 * con el. Nivel 15 para farol, 10 para farol de almas (igual que el bloque).
 */
public final class LanternLight {

    private record Spot(RegistryKey<World> world, BlockPos pos, int level) {
    }

    private static final Map<UUID, Spot> SPOTS = new HashMap<>();

    private LanternLight() {
    }

    /** @param lanternType 0 = sin farol, 1 = farol, 2 = farol de almas */
    public static void update(ServerPlayerEntity player, int lanternType) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        UUID id = player.getUuid();
        if (lanternType == 0 || !player.isAlive() || player.isSpectator()) {
            clear(server, id);
            return;
        }
        ServerWorld world = player.getServerWorld();
        BlockPos pos = BlockPos.ofFloored(player.getEyePos());
        int level = lanternType == 2 ? 10 : 15;

        Spot old = SPOTS.get(id);
        if (old != null && old.world().equals(world.getRegistryKey()) && old.pos().equals(pos) && old.level() == level) {
            return; // ya esta colocada
        }
        if (!world.isChunkLoaded(pos)) {
            return;
        }

        BlockState current = world.getBlockState(pos);
        boolean air = current.isAir();
        boolean water = current.isOf(Blocks.WATER);
        boolean ours = current.isOf(Blocks.LIGHT);
        if (air || water || ours) {
            boolean waterlogged = water || (ours && current.get(Properties.WATERLOGGED));
            BlockState light = Blocks.LIGHT.getDefaultState()
                    .with(LightBlock.LEVEL_15, level)
                    .with(Properties.WATERLOGGED, waterlogged);
            world.setBlockState(pos, light, Block.NOTIFY_LISTENERS);
            if (old != null && !(old.world().equals(world.getRegistryKey()) && old.pos().equals(pos))) {
                removeSpot(server, old);
            }
            SPOTS.put(id, new Spot(world.getRegistryKey(), pos.toImmutable(), level));
        } else if (old != null) {
            clear(server, id); // la cabeza esta dentro de un bloque solido: sin luz
        }
    }

    public static void clear(MinecraftServer server, UUID playerId) {
        Spot spot = SPOTS.remove(playerId);
        if (spot != null) {
            removeSpot(server, spot);
        }
    }

    public static void clearAll(MinecraftServer server) {
        for (Spot spot : new ArrayList<>(SPOTS.values())) {
            removeSpot(server, spot);
        }
        SPOTS.clear();
    }

    private static void removeSpot(MinecraftServer server, Spot spot) {
        ServerWorld world = server.getWorld(spot.world());
        if (world == null || !world.isChunkLoaded(spot.pos())) {
            return;
        }
        BlockState state = world.getBlockState(spot.pos());
        if (state.isOf(Blocks.LIGHT)) {
            BlockState replacement = state.get(Properties.WATERLOGGED)
                    ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState();
            world.setBlockState(spot.pos(), replacement, Block.NOTIFY_ALL);
        }
    }
}
