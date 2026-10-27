package net.climent.inmersivebackpack.mixin;

import net.climent.inmersivebackpack.sleep.BedrollManager;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin({LivingEntity.class, PlayerEntity.class})
public abstract class LivingEntityMixin {

    @Inject(method = "isSleepingInBed", at = @At("HEAD"), cancellable = true, require = 0)
    private void inmersivebackpack$allowBedroll(CallbackInfoReturnable<Boolean> cir) {
        Object self = this;
        if (!(self instanceof PlayerEntity player)) {
            return;
        }
        if (player.getWorld().isClient) {
            Optional<BlockPos> pos = player.getSleepingPosition();
            if (pos.isPresent() && !(player.getWorld().getBlockState(pos.get()).getBlock() instanceof BedBlock)) {
                cir.setReturnValue(true);
            }
        } else if (BedrollManager.isBedrollSleeper(player.getUuid())) {
            cir.setReturnValue(true);
        }
    }
}
