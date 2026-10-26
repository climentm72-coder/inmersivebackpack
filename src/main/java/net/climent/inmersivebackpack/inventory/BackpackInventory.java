package net.climent.inmersivebackpack.inventory;

import net.climent.inmersivebackpack.item.SmartBackpackItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

/**
 * Inventario de la mochila: 57 casillas.
 *
 *   0 .. 53  -> cofre doble (54)
 *  54 .. 55  -> correas para herramientas (2)
 *  56        -> manta de campana (1)
 *
 * Los datos se guardan en el NBT del propio ItemStack (clave "SmartBackpack"),
 * asi que viajan con la mochila: se puede soltar, guardar en un cofre, etc.
 */
public class BackpackInventory extends SimpleInventory {

    public static final int MAIN_SIZE = 54;
    public static final int TOOL_SLOT_START = 54;
    public static final int TOOL_SLOT_COUNT = 2;
    public static final int BEDROLL_SLOT = 56;
    public static final int TOTAL_SIZE = 57;

    private static final String NBT_KEY = "SmartBackpack";

    /** Tag de items que se aceptan como manta (editable por datapack). */
    public static final TagKey<Item> BEDROLLS =
            TagKey.of(RegistryKeys.ITEM, new Identifier("inmersivebackpack", "bedrolls"));

    /** ItemStack real de la mochila donde se persiste el contenido (vacio en el cliente). */
    private final ItemStack backing;
    private boolean loading = false;

    /** Inventario sin respaldo (lado cliente: el servidor sincroniza los slots). */
    public BackpackInventory() {
        this(ItemStack.EMPTY);
    }

    /** Inventario respaldado por el NBT de la mochila indicada. */
    public BackpackInventory(ItemStack backing) {
        super(TOTAL_SIZE);
        this.backing = backing;
        load();
    }

    // ------------------------------------------------------------------ reglas de slots

    public static boolean isMainSlot(int slot) {
        return slot >= 0 && slot < MAIN_SIZE;
    }

    public static boolean isToolSlot(int slot) {
        return slot >= TOOL_SLOT_START && slot < TOOL_SLOT_START + TOOL_SLOT_COUNT;
    }

    public static boolean isBedrollSlot(int slot) {
        return slot == BEDROLL_SLOT;
    }

    public static boolean isBedroll(ItemStack stack) {
        return stack.isIn(BEDROLLS);
    }

    /** Herramientas (todo lo que tenga durabilidad) mas algunos utilitarios. */
    public static boolean isToolOrUtility(ItemStack stack) {
        return stack.isDamageable()
                || stack.isOf(Items.COMPASS)
                || stack.isOf(Items.CLOCK)
                || stack.isOf(Items.SPYGLASS)
                || stack.isOf(Items.LEAD)
                || stack.isOf(Items.BUCKET)
                || stack.isOf(Items.BRUSH);
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        // La mochila NUNCA puede entrar en una mochila (evita bucles y duplicaciones).
        if (stack.getItem() instanceof SmartBackpackItem) {
            return false;
        }
        if (isBedrollSlot(slot)) {
            return isBedroll(stack);
        }
        if (isToolSlot(slot)) {
            return isToolOrUtility(stack);
        }
        return true;
    }

    // ------------------------------------------------------------------ persistencia NBT

    /** Guarda automaticamente cada vez que cambia algo (setStack, removeStack, etc.). */
    @Override
    public void markDirty() {
        super.markDirty();
        if (!loading) {
            save();
        }
    }

    @Override
    public void onClose(PlayerEntity player) {
        save();
    }

    /** Escribe las 57 casillas en el NBT de la mochila con Inventories.writeNbt. */
    public void save() {
        if (backing.isEmpty()) {
            return;
        }
        DefaultedList<ItemStack> list = DefaultedList.ofSize(TOTAL_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < TOTAL_SIZE; i++) {
            list.set(i, getStack(i));
        }
        NbtCompound tag = new NbtCompound();
        Inventories.writeNbt(tag, list, false); // false = no escribe "Items" si esta todo vacio
        if (tag.isEmpty()) {
            backing.removeSubNbt(NBT_KEY);
        } else {
            backing.setSubNbt(NBT_KEY, tag);
        }
    }

    /** Lee las 57 casillas del NBT de la mochila con Inventories.readNbt. */
    private void load() {
        if (backing.isEmpty()) {
            return;
        }
        NbtCompound tag = backing.getSubNbt(NBT_KEY);
        if (tag == null) {
            return;
        }
        DefaultedList<ItemStack> list = DefaultedList.ofSize(TOTAL_SIZE, ItemStack.EMPTY);
        Inventories.readNbt(tag, list);
        loading = true;
        try {
            for (int i = 0; i < TOTAL_SIZE; i++) {
                setStack(i, list.get(i));
            }
        } finally {
            loading = false;
        }
    }

    /** Lee solo la manta de una mochila sin montar un inventario completo. */
    public static ItemStack readBedroll(ItemStack backpack) {
        NbtCompound tag = backpack.getSubNbt(NBT_KEY);
        if (tag == null) {
            return ItemStack.EMPTY;
        }
        DefaultedList<ItemStack> list = DefaultedList.ofSize(TOTAL_SIZE, ItemStack.EMPTY);
        Inventories.readNbt(tag, list);
        return list.get(BEDROLL_SLOT);
    }
}
