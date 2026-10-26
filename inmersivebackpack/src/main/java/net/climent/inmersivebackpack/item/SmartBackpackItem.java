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
 * Mochila Medieval de Viajero (dos acabados: cuero de vaca y piel de conejo).
 *  - Clic derecho o tecla B: abre la mochila.
 *  - Shift + clic derecho sobre el suelo con manta equipada: duerme sobre la manta.
 * Sin durabilidad y sin penalizacion de peso/velocidad.
 */
public class SmartBackpackItem extends Item {

    public enum Variant { LEATHER, RABBIT }

    private final Variant variant;

    public SmartBackpackItem(Settings settings, Variant variant) {
        super(settings);
        this.variant = variant;
    }

    public Variant getVariant() {
        return variant;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            // Indice real del inventario: barra rapida 0-8, mano secundaria 40
            int lockedIndex = hand == Hand.MAIN_HAND ? user.getInventory().selectedSlot : 40;
            serverPlayer.openHandledScreen(new BackpackFactory(stack, lockedIndex));
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null || !player.isSneaking()) {
            return ActionResult.PASS; // sin Shift: se abre la mochila
        }
        ItemStack backpack = context.getStack();
        ItemStack bedroll = BackpackInventory.readSlot(backpack, BackpackInventory.BEDROLL_SLOT);
        if (bedroll.isEmpty()) {
            return ActionResult.PASS; // sin manta: se abre la mochila
        }
        World world = context.getWorld();
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (player instanceof ServerPlayerEntity serverPlayer) {
            BedrollManager.trySleep(serverPlayer, context.getBlockPos(), context.getSide(), bedroll);
        }
        return ActionResult.CONSUME;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.inmersivebackpack.open").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.inmersivebackpack.sleep").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.inmersivebackpack.guide").formatted(Formatting.DARK_GRAY));
    }

    /** Fabrica del menu: tambien le envia al cliente que casilla del inventario esta bloqueada. */
    public static class BackpackFactory implements ExtendedScreenHandlerFactory {
        private final ItemStack stack;
        private final int lockedIndex;

        public BackpackFactory(ItemStack stack, int lockedIndex) {
            this.stack = stack;
            this.lockedIndex = lockedIndex;
        }

        @Override
        public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
            buf.writeInt(lockedIndex);
        }

        @Override
        public Text getDisplayName() {
            return stack.getName();
        }

        @Override
        public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
            return new BackpackScreenHandler(syncId, playerInventory, new BackpackInventory(stack), stack, lockedIndex);
        }
    }
}
