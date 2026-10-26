package net.climent.inmersivebackpack.sleep;

import net.climent.inmersivebackpack.InmersiveBackpackMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CarpetBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manta de campana: dormir sin cama.
 *
 * Usa LivingEntity#sleep(BlockPos) directamente (no trySleep, que lee propiedades de un bloque cama
 * y fallaria sobre un bloque normal) y replica las comprobaciones de vanilla.
 *
 * Visual: una alfombra (BlockDisplay) del color de la cama se "despliega" bajo el jugador con una
 * animacion de escala al acostarse y se "recoge" al levantarse. Se elimina sola siempre.
 */
public final class BedrollManager {

    private static final String DISPLAY_TAG = "inmersivebackpack_bedroll_display";

    /**
     * AJUSTE: giro extra (grados) de la manta respecto al cuerpo tumbado del jugador.
     * Si al probar la manta sale cruzada respecto al cuerpo, prueba 90, 180 o 270.
     */
    private static final float BLANKET_YAW_OFFSET = 0.0f;
    private static final double BLANKET_LENGTH = 2.0;
    private static final double BLANKET_WIDTH = 1.1;
    /** Mitad de la longitud del cuerpo tumbado (el cuerpo se extiende desde los pies hacia la cabeza). */
    private static final double BODY_HALF = 0.9;

    private static final int GROW_TICKS = 20;
    private static final int SHRINK_TICKS = 10;

    private static final class Blanket {
        final UUID display;
        final float theta;
        final int spawnTick;
        boolean grown = false;

        Blanket(UUID display, float theta, int spawnTick) {
            this.display = display;
            this.theta = theta;
            this.spawnTick = spawnTick;
        }
    }

    private static final Set<UUID> SLEEPERS = new HashSet<>();
    private static final Map<UUID, Blanket> BLANKETS = new HashMap<>();
    /** uuid de la manta que se esta recogiendo -> tick en que se elimina */
    private static final Map<UUID, Integer> FADING = new HashMap<>();

    private BedrollManager() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(BedrollManager::tick);

        // Elimina mantas huerfanas (servidor cerrado mientras alguien dormia)
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity.getCommandTags().contains(DISPLAY_TAG) && !isKnown(entity.getUuid())) {
                world.getServer().execute(entity::discard);
            }
        });
    }

    private static boolean isKnown(UUID displayId) {
        if (FADING.containsKey(displayId)) {
            return true;
        }
        for (Blanket b : BLANKETS.values()) {
            if (b.display.equals(displayId)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBedrollSleeper(UUID playerId) {
        return SLEEPERS.contains(playerId);
    }

    // ------------------------------------------------------------------ dormir

    public static void trySleep(ServerPlayerEntity player, BlockPos groundPos, Direction side, ItemStack bedroll) {
        ServerWorld world = player.getServerWorld();

        if (player.isSleeping() || !player.isAlive()) {
            return;
        }
        if (!world.getDimension().natural()) {
            msg(player, "not_here");
            return;
        }
        BlockState ground = world.getBlockState(groundPos);
        if (side != Direction.UP || !ground.isSideSolidFullSquare(world, groundPos, Direction.UP)) {
            msg(player, "bad_surface");
            return;
        }
        BlockPos sleepPos = groundPos.up();
        if (!world.getBlockState(sleepPos).getCollisionShape(world, sleepPos).isEmpty()
                || !world.getFluidState(sleepPos).isEmpty()) {
            msg(player, "no_space");
            return;
        }
        if (world.isDay()) {
            msg(player, "too_early");
            return;
        }
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
        player.sleep(sleepPos);
        SLEEPERS.add(player.getUuid());

        double x = sleepPos.getX() + 0.5;
        double y = sleepPos.getY() + 0.0625;
        double z = sleepPos.getZ() + 0.5;
        player.setPosition(x, y, z);
        player.networkHandler.requestTeleport(x, y, z, player.getYaw(), player.getPitch());

        spawnBlanket(world, player, bedroll, x, sleepPos.getY(), z, player.bodyYaw);

        world.playSound(null, sleepPos, SoundEvents.BLOCK_WOOL_PLACE, SoundCategory.PLAYERS, 1.0f, 0.9f);
        world.updateSleepingPlayers();
        msg(player, "deployed");
    }

    private static void msg(ServerPlayerEntity player, String key) {
        player.sendMessage(Text.translatable("message.inmersivebackpack.bedroll." + key), true);
    }

    // ------------------------------------------------------------------ manta visual

    /** Alfombra del color de la cama (o la propia alfombra); blanca por defecto. */
    private static Block carpetFor(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof BedBlock bed) {
                DyeColor color = bed.getColor();
                Block carpet = Registries.BLOCK.get(new Identifier(color.getName() + "_carpet"));
                if (carpet != Blocks.AIR) {
                    return carpet;
                }
            } else if (block instanceof CarpetBlock) {
                return block;
            }
        }
        return Blocks.WHITE_CARPET;
    }

    /**
     * Transformacion de la alfombra para un tamano relativo {@code s} (0.05 = recogida, 1 = desplegada),
     * siempre centrada en el centro del cuerpo tumbado. theta = bodyYaw del jugador en grados.
     */
    private static NbtCompound transform(float theta, float s) {
        double th = Math.toRadians(theta);
        double alpha = th + Math.PI + Math.toRadians(BLANKET_YAW_OFFSET);

        // Centro del cuerpo respecto al jugador (los pies estan en el origen, la cabeza hacia h)
        double cx = BODY_HALF * -Math.cos(th);
        double cz = BODY_HALF * Math.sin(th);

        double sx = BLANKET_LENGTH * s;
        double sy = s;
        double sz = BLANKET_WIDTH * s;
        // Centro de la alfombra escalada, antes de girar
        double bx = sx / 2.0;
        double bz = sz / 2.0;
        double rx = bx * Math.cos(alpha) + bz * Math.sin(alpha);
        double rz = -bx * Math.sin(alpha) + bz * Math.cos(alpha);

        NbtCompound t = new NbtCompound();
        t.put("translation", floats((float) (cx - rx), 0.005f, (float) (cz - rz)));
        t.put("scale", floats((float) sx, (float) sy, (float) sz));
        t.put("left_rotation", floats(0f, (float) Math.sin(alpha / 2.0), 0f, (float) Math.cos(alpha / 2.0)));
        t.put("right_rotation", floats(0f, 0f, 0f, 1f));
        return t;
    }

    private static void spawnBlanket(ServerWorld world, ServerPlayerEntity player, ItemStack bedroll,
                                     double x, double groundY, double z, float theta) {
        try {
            DisplayEntity.BlockDisplayEntity display = EntityType.BLOCK_DISPLAY.create(world);
            if (display == null) {
                return;
            }
            NbtCompound blockState = new NbtCompound();
            blockState.putString("Name", Registries.BLOCK.getId(carpetFor(bedroll)).toString());

            NbtCompound nbt = new NbtCompound();
            nbt.put("block_state", blockState);
            nbt.put("transformation", transform(theta, 0.05f));
            display.readNbt(nbt);

            display.refreshPositionAndAngles(x, groundY, z, 0f, 0f);
            display.addCommandTag(DISPLAY_TAG);
            display.setInvulnerable(true);

            BLANKETS.put(player.getUuid(), new Blanket(display.getUuid(), theta, world.getServer().getTicks()));
            if (!world.spawnEntity(display)) {
                BLANKETS.remove(player.getUuid());
            }
        } catch (Exception e) {
            BLANKETS.remove(player.getUuid());
            InmersiveBackpackMod.LOGGER.warn(
                    "No se pudo mostrar la manta (el jugador duerme igualmente): {}", e.toString());
        }
    }

    private static void animate(Entity display, float theta, float s, int duration) {
        try {
            NbtCompound nbt = display.writeNbt(new NbtCompound());
            nbt.put("transformation", transform(theta, s));
            nbt.putInt("start_interpolation", 0);
            nbt.putInt("interpolation_duration", duration);
            display.readNbt(nbt);
        } catch (Exception e) {
            InmersiveBackpackMod.LOGGER.warn("No se pudo animar la manta: {}", e.toString());
        }
    }

    private static NbtList floats(float... values) {
        NbtList list = new NbtList();
        for (float v : values) {
            list.add(NbtFloat.of(v));
        }
        return list;
    }

    private static Entity find(MinecraftServer server, UUID id) {
        for (ServerWorld w : server.getWorlds()) {
            Entity e = w.getEntity(id);
            if (e != null) {
                return e;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ tick y limpieza

    private static void tick(MinecraftServer server) {
        final int now = server.getTicks();

        if (!SLEEPERS.isEmpty()) {
            Iterator<UUID> it = SLEEPERS.iterator();
            while (it.hasNext()) {
                UUID id = it.next();
                ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
                Blanket blanket = BLANKETS.get(id);

                if (p == null || p.isRemoved() || !p.isSleeping()) {
                    // Levantarse: la manta se recoge y desaparece
                    if (blanket != null) {
                        Entity e = find(server, blanket.display);
                        if (e == null || p == null) {
                            if (e != null) {
                                e.discard();
                            }
                        } else {
                            animate(e, blanket.theta, 0.05f, SHRINK_TICKS);
                            FADING.put(blanket.display, now + SHRINK_TICKS);
                        }
                        BLANKETS.remove(id);
                    }
                    it.remove();
                    continue;
                }

                // Acostarse: tras 2 ticks la manta se despliega con una animacion
                if (blanket != null && !blanket.grown && now - blanket.spawnTick >= 2) {
                    Entity e = find(server, blanket.display);
                    if (e != null) {
                        animate(e, blanket.theta, 1.0f, GROW_TICKS);
                    }
                    blanket.grown = true;
                }
            }
        }

        if (!FADING.isEmpty()) {
            Iterator<Map.Entry<UUID, Integer>> it = FADING.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Integer> entry = it.next();
                if (now >= entry.getValue()) {
                    Entity e = find(server, entry.getKey());
                    if (e != null) {
                        e.discard();
                    }
                    it.remove();
                }
            }
        }
    }
}
