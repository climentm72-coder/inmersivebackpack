package net.climent.inmersivebackpack.item;

import net.climent.inmersivebackpack.inventory.BackpackInventory;
import net.climent.inmersivebackpack.screen.BackpackScreenHandler;
import net.climent.inmersivebackpack.sleep.BedrollManager;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/**
 * Mochila Medieval de Viajero.
 *  - Clic derecho: abre la mochila (54 + 2 correas + manta).
 *  - Shift + clic derecho sobre el suelo con manta equipada: duerme sobre la manta.
 *
 * Sin durabilidad (no se define maxDamage) y sin penalizacion de peso/velocidad.
 */
public class SmartBackpackItem extends Item {

    public SmartBackpackItem(Settings settings) {
        super(settings);
    }

    // ------------------------------------------------------------------ abrir el menu

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            // Si esta en la mano principal, recordamos el slot de la barra rapida para bloquearlo
            // dentro del menu; mano secundaria = -1.
            int lockedIndex = hand == Hand.MAIN_HAND ? user.getInventory().selectedSlot : -1;
            serverPlayer.openHandledScreen(new BackpackFactory(stack, lockedIndex));
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    // ------------------------------------------------------------------ Shift + clic = dormir

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        // Sin Shift: PASS -> el juego llama a use() y se abre la mochila.
        if (player == null || !player.isSneaking()) {
            return ActionResult.PASS;
        }
        ItemStack backpack = context.getStack();
        // Sin manta equipada: PASS tambien, se abre la mochila como siempre.
        if (BackpackInventory.readBedroll(backpack).isEmpty()) {
            return ActionResult.PASS;
        }
        World world = context.getWorld();
        if (world.isClient) {
            return ActionResult.SUCCESS; // animacion de mano; la logica real es del servidor
        }
        if (player instanceof ServerPlayerEntity serverPlayer) {
            BedrollManager.trySleep(serverPlayer, context.getBlockPos(), context.getSide(),
                    BackpackInventory.readBedroll(backpack));
        }
        return ActionResult.CONSUME;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.inmersivebackpack.smart_backpack.open").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.inmersivebackpack.smart_backpack.sleep").formatted(Formatting.GRAY));
    }

    // ------------------------------------------------------------------ fabrica del menu

    /** Abre el menu y envia al cliente el slot bloqueado (lo lee el constructor del cliente). */
    private static class BackpackFactory implements ExtendedScreenHandlerFactory {
        private final ItemStack stack;
        private final int lockedIndex;

        BackpackFactory(ItemStack stack, int lockedIndex) {
            this.stack = stack;
            this.lockedIndex = lockedIndex;
        }

        @Override
        public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
            buf.writeInt(lockedIndex);
        }

        @Override
        public Text getDisplayName() {
            return Text.translatable("item.inmersivebackpack.smart_backpack");
        }

        @Override
        public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
            return new BackpackScreenHandler(syncId, playerInventory, new BackpackInventory(stack), stack, lockedIndex);
        }
    }
}
