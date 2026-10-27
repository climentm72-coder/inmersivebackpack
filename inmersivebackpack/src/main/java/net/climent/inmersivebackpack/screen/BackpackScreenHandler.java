Aquí tienes el archivo **`BackpackScreenHandler.java`** completo y corregido, listo para que lo copies, lo pegues en tu repositorio de GitHub y funcione correctamente:

```java
package net.climent.inmersivebackpack.screen;

import net.climent.inmersivebackpack.InmersiveBackpackMod;
import net.climent.inmersivebackpack.inventory.BackpackInventory;
import net.climent.inmersivebackpack.item.SmartBackpackItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Menu de la mochila: 56 slots de mochila + 36 del jugador = 92 slots.
 *
 *   0..53    cofre doble | 54 farol | 55 manta | 56..82 inventario | 83..91 barra rapida
 */
public class BackpackScreenHandler extends ScreenHandler {

    public static final int SLOT_SIZE = 18;
    public static final int GRID_X = 8;
    public static final int GRID_Y = 18;
    public static final int GRID_COLUMNS = 9;
    public static final int GRID_ROWS = 6;
    public static final int EXTRAS_Y = GRID_Y + GRID_ROWS * SLOT_SIZE + 6;
    public static final int PLAYER_INV_Y = EXTRAS_Y + SLOT_SIZE + 14;
    public static final int HOTBAR_Y = PLAYER_INV_Y + 58;
    public static final int BACKGROUND_WIDTH = 176;
    public static final int BACKGROUND_HEIGHT = HOTBAR_Y + SLOT_SIZE + 7;

    private static final int PLAYER_SLOTS_START = BackpackInventory.TOTAL_SIZE;

    private final BackpackInventory inventory;
    private final ItemStack backpackStack;
    /** Indice del inventario del jugador donde esta la mochila abierta (0-8 barra, 40 mano secundaria). */
    private final int lockedIndex;

    /** Constructor del CLIENTE (ExtendedScreenHandlerType). */
    public BackpackScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, new BackpackInventory(), ItemStack.EMPTY, buf.readInt());
    }

    /** Constructor del SERVIDOR. */
    public BackpackScreenHandler(int syncId, PlayerInventory playerInventory,
                                 BackpackInventory inventory, ItemStack backpackStack, int lockedIndex) {
        super(InmersiveBackpackMod.BACKPACK_SCREEN_HANDLER, syncId);
        checkSize(inventory, BackpackInventory.TOTAL_SIZE);
        this.inventory = inventory;
        this.backpackStack = backpackStack;
        this.lockedIndex = lockedIndex;
        inventory.onOpen(playerInventory.player);

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                this.addSlot(new BackpackSlot(inventory, col + row * GRID_COLUMNS,
                        GRID_X + col * SLOT_SIZE, GRID_Y + row * SLOT_SIZE));
            }
        }
        // Casilla azul de farol (indice 54) y casilla roja de manta (indice 55)
        this.addSlot(new BackpackSlot(inventory, BackpackInventory.LANTERN_SLOT, GRID_X, EXTRAS_Y));
        this.addSlot(new BackpackSlot(inventory, BackpackInventory.BEDROLL_SLOT,
                GRID_X + 8 * SLOT_SIZE, EXTRAS_Y));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new PlayerSlot(playerInventory, col + row * 9 + 9,
                        GRID_X + col * SLOT_SIZE, PLAYER_INV_Y + row * SLOT_SIZE));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new PlayerSlot(playerInventory, col, GRID_X + col * SLOT_SIZE, HOTBAR_Y));
        }
    }

    private static class BackpackSlot extends Slot {
        BackpackSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return this.inventory.isValid(this.getIndex(), stack);
        }

        @Override
        public int getMaxItemCount() {
            // Farol y manta: un solo item por casilla
            return this.getIndex() >= BackpackInventory.LANTERN_SLOT ? 1 : super.getMaxItemCount();
        }
    }

    /** Slot del jugador: no deja sacar de su sitio la mochila que esta abierta. */
    private class PlayerSlot extends Slot {
        PlayerSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canTakeItems(PlayerEntity player) {
            return !(this.inventory instanceof PlayerInventory && this.getIndex() == lockedIndex);
        }
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        // Cambio con tecla numerica o tecla F: no sacar la mochila abierta de su sitio
        if (actionType == SlotActionType.SWAP && button == lockedIndex) {
            return;
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    /**
     * Shift + clic. Una mochila JAMAS se mueve con shift-click (ni dentro ni fuera),
     * asi no puede meterse en su propio inventario ni generar bucles de duplicacion.
     */
    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getStack();

        if (stack.getItem() instanceof SmartBackpackItem) {
            return ItemStack.EMPTY;
        }

        ItemStack original = stack.copy();

        if (slotIndex < PLAYER_SLOTS_START) {
            if (!this.insertItem(stack, PLAYER_SLOTS_START, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Orden: farol, manta, cofre. insertItem respeta canInsert de cada slot.
            boolean moved = this.insertItem(stack, BackpackInventory.LANTERN_SLOT, BackpackInventory.LANTERN_SLOT + 1, false);
            if (!stack.isEmpty()) {
                moved |= this.insertItem(stack, BackpackInventory.BEDROLL_SLOT, BackpackInventory.BEDROLL_SLOT + 1, false);
            }
            if (!stack.isEmpty()) {
                moved |= this.insertItem(stack, 0, BackpackInventory.MAIN_SIZE, false);
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }
        return original;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        if (backpackStack.isEmpty()) {
            return true; // cliente
        }
        if (lockedIndex < 0 || lockedIndex >= player.getInventory().size()) {
            return true;
        }
        return ItemStack.areEqual(player.getInventory().getStack(lockedIndex), backpackStack);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        inventory.onClose(player);
    }

    public BackpackInventory getBackpackInventory() {
        return inventory;
    }
}

```
