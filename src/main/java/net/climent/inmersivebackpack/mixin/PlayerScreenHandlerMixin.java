package net.climent.inmersivebackpack.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerScreenHandler.class)
public class PlayerScreenHandlerMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void addBackpackSlot(PlayerInventory inventory, boolean onServer, PlayerEntity owner, CallbackInfo ci) {
        // Añade la ranura 41 en las coordenadas del inventario (ejemplo encima del escudo)
        ((PlayerScreenHandler) (Object) this).addSlot(new Slot(inventory, 41, 77, 62) {
            @Override
            public boolean canInsert(net.minecraft.item.ItemStack stack) {
                return stack.getItem() instanceof net.climent.inmersivebackpack.item.SmartBackpackItem;
            }

            @Override
            public int getMaxItemCount() {
                return 1;
            }
        });
    }
}
