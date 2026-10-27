package net.climent.inmersivebackpack.util;

import net.climent.inmersivebackpack.inventory.BackpackInventory;
import net.climent.inmersivebackpack.item.SmartBackpackItem;
import net.climent.inmersivebackpack.network.WornInfo;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;

/** Utilidades para localizar la mochila que lleva puesto un jugador. */
public final class BackpackUtil {

    private BackpackUtil() {
    }

    /**
     * Índice (en el inventario del jugador) de la primera mochila que lleva encima:
     * ranura dedicada (41), barra rápida e inventario (0-35) o mano secundaria (40). -1 si no lleva ninguna.
     */
    public static int findWornIndex(PlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        // Primero comprobamos la ranura exclusiva de la mochila (41)
        if (inv.getStack(41).getItem() instanceof SmartBackpackItem) {
            return 41;
        }
        for (int i = 0; i < inv.main.size(); i++) {
            if (inv.getStack(i).getItem() instanceof SmartBackpackItem) {
                return i;
            }
        }
        if (inv.getStack(40).getItem() instanceof SmartBackpackItem) {
            return 40;
        }
        return -1;
    }

    public static WornInfo info(PlayerEntity player) {
        int idx = findWornIndex(player);
        if (idx < 0) {
            return WornInfo.NONE;
        }
        ItemStack backpack = player.getInventory().getStack(idx);
        SmartBackpackItem item = (SmartBackpackItem) backpack.getItem();
        int variant = item.getVariant() == SmartBackpackItem.Variant.RABBIT ? 2 : 1;

        ItemStack lantern = BackpackInventory.readSlot(backpack, BackpackInventory.LANTERN_SLOT);
        int lanternType = lantern.isOf(Items.LANTERN) ? 1 : lantern.isOf(Items.SOUL_LANTERN) ? 2 : 0;

        boolean bedroll = !BackpackInventory.readSlot(backpack, BackpackInventory.BEDROLL_SLOT).isEmpty();
        return new WornInfo(variant, lanternType, bedroll);
    }
}
