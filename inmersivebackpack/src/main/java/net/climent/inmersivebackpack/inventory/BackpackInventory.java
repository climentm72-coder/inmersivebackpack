package net.climent.inmersivebackpack.inventory;

import net.climent.inmersivebackpack.item.SmartBackpackItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

/**
 * Inventario de la mochila: 56 casillas.
 *
 *   0 .. 53  -> cofre doble (54)
 *  54        -> farol (1, solo faroles)
 *  55        -> manta de campana (1)
 *
 * Se guarda en el NBT del propio ItemStack (clave "SmartBackpack"), asi que viaja con la mochila.
 */
public class BackpackInventory extends SimpleInventory {

    public static final int MAIN_SIZE = 54;
    public static final int LANTERN_SLOT = 54;
    public static final int BEDROLL_SLOT = 55;
    public static final int TOTAL_SIZE = 56;

    private static final String NBT_KEY = "SmartBackpack";

    /** Items aceptados como manta (editable por datapack). */
    public static final TagKey<Item> BEDROLLS =
            TagKey.of(RegistryKeys.ITEM, new Identifier("inmersivebackpack", "bedrolls"));

    private final ItemStack backing;
    private boolean loading = false;

    /** Inventario sin respaldo (cliente: el servidor sincroniza los slots). */
    public BackpackInventory() {
        this(ItemStack.EMPTY);
    }

    /** Inventario respaldado por el NBT de la mochila indicada. */
    public BackpackInventory(ItemStack backing) {
        super(TOTAL_SIZE);
        this.backing = backing;
        load();
    }

    public static boolean isLanternSlot(int slot) {
        return slot == LANTERN_SLOT;
    }

    public static boolean isBedrollSlot(int slot) {
        return slot == BEDROLL_SLOT;
    }

    public static boolean isLantern(ItemStack stack) {
        return stack.isOf(Items.LANTERN) || stack.isOf(Items.SOUL_LANTERN);
    }

    public static boolean isBedroll(ItemStack stack) {
        return stack.isIn(BEDROLLS);
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        // La mochila NUNCA puede entrar en una mochila (evita bucles y duplicaciones).
        if (stack.getItem() instanceof SmartBackpackItem) {
            return false;
        }
        if (isLanternSlot(slot)) {
            return isLantern(stack);
        }
        if (isBedrollSlot(slot)) {
            return isBedroll(stack);
        }
        return true;
    }

    // ------------------------------------------------------------------ persistencia NBT

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

    public void save() {
        if (backing.isEmpty()) {
            return;
        }
        DefaultedList<ItemStack> list = DefaultedList.ofSize(TOTAL_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < TOTAL_SIZE; i++) {
            list.set(i, getStack(i));
        }
        NbtCompound tag = new NbtCompound();
        Inventories.writeNbt(tag, list, false);
        if (tag.isEmpty()) {
            backing.removeSubNbt(NBT_KEY);
        } else {
            backing.setSubNbt(NBT_KEY, tag);
        }
    }

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

    /** Lee una sola casilla de una mochila sin montar el inventario completo (barato). */
    public static ItemStack readSlot(ItemStack backpack, int slot) {
        NbtCompound tag = backpack.getSubNbt(NBT_KEY);
        if (tag == null) {
            return ItemStack.EMPTY;
        }
        NbtList list = tag.getList("Items", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            if ((entry.getByte("Slot") & 255) == slot) {
                return ItemStack.fromNbt(entry);
            }
        }
        return ItemStack.EMPTY;
    }
}
