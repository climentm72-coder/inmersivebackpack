package net.climent.inmersivebackpack.sleep;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.block.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Logica de la manta de campana: dormir sin cama.
 *
 * Usa LivingEntity#sleep(BlockPos) directamente (NO ServerPlayerEntity#trySleep, que lee la
 * propiedad FACING de un bloque cama y crashearia sobre un bloque normal). Por eso replica
 * aqui las comprobaciones de vanilla: dimension, hora, espacio libre y monstruos cerca.
 *
 * Visual: se spawnea un ItemDisplay (entidad vanilla) con la manta tumbada bajo el jugador.
 * Se elimina al despertar, al desconectar, o si queda huerfana tras un cierre del servidor.
 */
public final class BedrollManager {

    private static final String DISPLAY_TAG = "inmersivebackpack_bedroll_display";

    /** Ajuste fino del giro de la manta respecto al jugador (grados). Cambialo si queda girada. */
    private static final float BLANKET_YAW_OFFSET = 0.0f;

    /** Jugadores durmiendo sobre manta (la comprueba el mixin para no despertarlos). */
    private static final Set<UUID> SLEEPERS = new HashSet<>();
    /** jugador -> entidad display de su manta. */
    private static final Map<UUID, UUID> DISPLAYS = new HashMap<>();

    private BedrollManager() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(BedrollManager::tick);

        // Limpia mantas huerfanas (servidor cerrado mientras alguien dormia)
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity.getCommandTags().contains(DISPLAY_TAG) && !DISPLAYS.containsValue(entity.getUuid())) {
                world.getServer().execute(entity::discard);
            }
        });
    }

    /** Lo consulta el mixin: true si este jugador esta durmiendo sobre una manta. */
    public static boolean isBedrollSleeper(UUID playerId) {
        return SLEEPERS.contains(playerId);
    }

    // ------------------------------------------------------------------ dormir

    public static void trySleep(ServerPlayerEntity player, BlockPos groundPos, Direction side, ItemStack bedroll) {
        ServerWorld world = player.getServerWorld();

        if (player.isSleeping() || !player.isAlive()) {
            return;
        }
        // Igual que las camas: solo en dimensiones "naturales" (Overworld)
        if (!world.getDimension().natural()) {
            msg(player, "not_here");
            return;
        }
        // Solo sobre la cara superior de un bloque con superficie solida
        BlockState ground = world.getBlockState(groundPos);
        if (side != Direction.UP || !ground.isSideSolidFullSquare(world, groundPos, Direction.UP)) {
            msg(player, "bad_surface");
            return;
        }
        // Espacio libre para tumbarse
        BlockPos sleepPos = groundPos.up();
        if (!world.getBlockState(sleepPos).getCollisionShape(world, sleepPos).isEmpty()
                || !world.getFluidState(sleepPos).isEmpty()) {
            msg(player, "no_space");
            return;
        }
        // Solo de noche o con tormenta (como una cama normal)
        if (world.isDay()) {
            msg(player, "too_early");
            return;
        }
        // Monstruos cerca (mismo area que vanilla: 8 x 5 x 8)
        List<HostileEntity> monsters = world.getEntitiesByClass(
                HostileEntity.class,
                new Box(sleepPos.getX() - 8, sleepPos.getY() - 5, sleepPos.getZ() - 8,
                        sleepPos.getX() + 9, sleepPos.getY() + 6, sleepPos.getZ() + 9),
                hostile -> hostile.isAngryAt(player));
        if (!monsters.isEmpty()) {
            msg(player, "monsters");
            return;
        }

        // --- Dormir ---
        player.sleep(sleepPos); // LivingEntity#sleep: pose SLEEPING + posicion de sueno
        SLEEPERS.add(player.getUuid());

        // sleep() coloca al jugador a la altura de una cama (+0.6875); aqui es el suelo.
        double x = sleepPos.getX() + 0.5;
        double y = sleepPos.getY() + 0.0625;
        double z = sleepPos.getZ() + 0.5;
        player.setPosition(x, y, z);
        player.networkHandler.requestTeleport(x, y, z, player.getYaw(), player.getPitch());

        spawnBlanket(world, player, bedroll, x, y, z);

        world.playSound(null, sleepPos, SoundEvents.BLOCK_WOOL_PLACE, SoundCategory.PLAYERS, 1.0f, 0.9f);
        // Avisa al servidor: si todos duermen, se salta la noche con normalidad
        world.updateSleepingPlayers();
        msg(player, "deployed");
    }

    private static void msg(ServerPlayerEntity player, String key) {
        player.sendMessage(Text.translatable("message.inmersivebackpack.bedroll." + key), true);
    }

    // ------------------------------------------------------------------ manta visual

    private static void spawnBlanket(ServerWorld world, ServerPlayerEntity player, ItemStack bedroll,
                                     double x, double y, double z) {
        try {
            DisplayEntity.ItemDisplayEntity display = EntityType.ITEM_DISPLAY.create(world);
            if (display == null) {
                return;
            }
            ItemStack single = bedroll.copy();
            single.setCount(1);

            NbtCompound itemNbt = new NbtCompound();
            single.writeNbt(itemNbt);

            // Transformacion: tumbada (giro -90 grados en X) y al doble de tamano
            NbtCompound transformation = new NbtCompound();
            transformation.put("translation", floats(0f, 0.01f, 0f));
            transformation.put("scale", floats(2f, 2f, 2f));
            transformation.put("left_rotation", floats(-0.70710678f, 0f, 0f, 0.70710678f));
            transformation.put("right_rotation", floats(0f, 0f, 0f, 1f));

            NbtCompound nbt = new NbtCompound();
            nbt.put("item", itemNbt);
            nbt.putString("item_display", "fixed");
            nbt.put("transformation", transformation);
            display.readNbt(nbt);

            display.refreshPositionAndAngles(x, y, z, player.getBodyYaw() + BLANKET_YAW_OFFSET, 0f);
            display.addCommandTag(DISPLAY_TAG);
            display.setInvulnerable(true);

            // Registrar ANTES de spawnear: el evento ENTITY_LOAD no debe tomarla por huerfana
            DISPLAYS.put(player.getUuid(), display.getUuid());
            if (!world.spawnEntity(display)) {
                DISPLAYS.remove(player.getUuid());
            }
        } catch (Exception e) {
            DISPLAYS.remove(player.getUuid());
            net.climent.inmersivebackpack.InmersiveBackpackMod.LOGGER.warn(
                    "No se pudo mostrar la manta (el jugador duerme igualmente): {}", e.toString());
        }
    }

    private static NbtList floats(float... values) {
        NbtList list = new NbtList();
        for (float v : values) {
            list.add(NbtFloat.of(v));
        }
        return list;
    }

    // ------------------------------------------------------------------ limpieza

    private static void tick(MinecraftServer server) {
        if (SLEEPERS.isEmpty()) {
            return;
        }
        Iterator<UUID> it = SLEEPERS.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
            if (p == null || p.isRemoved() || !p.isSleeping()) {
                removeBlanket(server, id);
                it.remove();
            }
        }
    }

    private static void removeBlanket(MinecraftServer server, UUID playerId) {
        UUID displayId = DISPLAYS.remove(playerId);
        if (displayId == null) {
            return;
        }
        for (ServerWorld w : server.getWorlds()) {
            Entity e = w.getEntity(displayId);
            if (e != null) {
                e.discard();
                return;
            }
        }
    }
}
