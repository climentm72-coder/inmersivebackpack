
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
 * Menu de la mochila: 59 slots de mochila + 36 del jugador = 95 slots.
 *
 *   indices   0..53   cofre doble
 *   indices  54..55   correas de herramientas
 *   indices  56..57   soportes para faroles
 *   indice     58     manta / cama
 *   indices  59..85   inventario del jugador (27)
 *   indices  86..94   barra rapida (9)
 */
public class BackpackScreenHandler extends ScreenHandler {

    // ---- Geometria compartida con la pantalla del cliente ----
    public static final int SLOT_SIZE = 18;
    public static final int GRID_X = 8;
    public static final int GRID_Y = 18;
    public static final int GRID_COLUMNS = 9;
    public static final int GRID_ROWS = 6;
    public static final int EXTRAS_Y = GRID_Y + GRID_ROWS * SLOT_SIZE + 6;   // fila de correas, faroles y manta
    public static final int PLAYER_INV_Y = EXTRAS_Y + SLOT_SIZE + 14;
    public static final int HOTBAR_Y = PLAYER_INV_Y + 58;
    public static final int BACKGROUND_WIDTH = 176;
    public static final int BACKGROUND_HEIGHT = HOTBAR_Y + SLOT_SIZE + 7;

    private static final int PLAYER_SLOTS_START = BackpackInventory.TOTAL_SIZE; // 59

    private final BackpackInventory inventory;
    /** Stack real de la mochila (en el cliente es EMPTY). */
    private final ItemStack backpackStack;
    /**
     * Indice (0-8) de la barra rapida donde esta la mochila abierta,
     * o -1 si se abrio desde la mano secundaria.
     */
    private final int lockedIndex;

    /** Constructor del CLIENTE: lo llama ExtendedScreenHandlerType con los datos del servidor. */
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

        // 1. Cofre doble: 6 filas x 9 columnas (indices 0..53)
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                this.addSlot(new BackpackSlot(inventory, col + row * GRID_COLUMNS,
                        GRID_X + col * SLOT_SIZE, GRID_Y + row * SLOT_SIZE));
            }
        }

        // 2. Correas de herramientas (indices 54 y 55)
        this.addSlot(new BackpackSlot(inventory, BackpackInventory.TOOL_SLOT_START, GRID_X, EXTRAS_Y));
        this.addSlot(new BackpackSlot(inventory, BackpackInventory.TOOL_SLOT_START + 1, GRID_X + SLOT_SIZE, EXTRAS_Y));

        // 3. Soportes para faroles (indices 56 y 57), justo al lado
        this.addSlot(new BackpackSlot(inventory, BackpackInventory.LANTERN_SLOT_START, GRID_X + 2 * SLOT_SIZE, EXTRAS_Y));
        this.addSlot(new BackpackSlot(inventory, BackpackInventory.LANTERN_SLOT_START + 1, GRID_X + 3 * SLOT_SIZE, EXTRAS_Y));

        // 4. Manta de campana / cama (indice 58), alineada a la derecha
        this.addSlot(new BackpackSlot(inventory, BackpackInventory.BEDROLL_SLOT,
                GRID_X + 8 * SLOT_SIZE, EXTRAS_Y));

        // 5. Inventario del jugador (3 filas) y barra rapida
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

    // ------------------------------------------------------------------ slots propios

    /** Slot de la mochila: respeta las reglas de BackpackInventory.isValid y limita extras a 1 item. */
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
            // Extras (herramientas, faroles y manta): un solo item por casilla
            return this.getIndex() >= BackpackInventory.TOOL_SLOT_START ? 1 : super.getMaxItemCount();
        }
    }

    /** Slot del jugador: no deja sacar de su sitio la mochila que esta abierta. */
    private class PlayerSlot extends Slot {
        PlayerSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canTakeItems(PlayerEntity player) {
            return !isLockedBackpackSlot(this);
        }
    }

    private boolean isLockedBackpackSlot(Slot slot) {
        return lockedIndex >= 0
                && slot.inventory instanceof PlayerInventory
                && slot.getIndex() == lockedIndex;
    }

    // ------------------------------------------------------------------ logica del menu

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (actionType == SlotActionType.SWAP) {
            if (button == lockedIndex || (lockedIndex < 0 && button == 40)) {
                return;
            }
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

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
            // Mochila -> jugador
            if (!this.insertItem(stack, PLAYER_SLOTS_START, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Jugador -> mochila. Orden: manta, faroles, herramientas, cofre.
            boolean moved = this.insertItem(stack, BackpackInventory.BEDROLL_SLOT, BackpackInventory.BEDROLL_SLOT + 1, false);
            if (!stack.isEmpty()) {
                moved |= this.insertItem(stack, BackpackInventory.LANTERN_SLOT_START,
                        BackpackInventory.LANTERN_SLOT_START + BackpackInventory.LANTERN_SLOT_COUNT, false);
            }
            if (!stack.isEmpty()) {
                moved |= this.insertItem(stack, BackpackInventory.TOOL_SLOT_START,
                        BackpackInventory.TOOL_SLOT_START + BackpackInventory.TOOL_SLOT_COUNT, false);
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
            return true;
        }
        ItemStack held = lockedIndex >= 0
                ? player.getInventory().getStack(lockedIndex)
                : player.getOffHandStack();
        return held == backpackStack;
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
