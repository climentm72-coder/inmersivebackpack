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

/**
 * Vanilla despierta a cualquier entidad dormida cuyo bloque de dormir no sea una cama
 * (LivingEntity#isSleepingInBed). Para que la manta de campana funcione sin bloque de cama,
 * este mixin hace que esa comprobacion devuelva true para jugadores que duermen en manta.
 *
 *  - Servidor: solo para jugadores registrados en BedrollManager.
 *  - Cliente: si el jugador duerme sobre un bloque que no es cama (el servidor es quien manda).
 *
 * require = 0: si esa comprobacion no existe con ese nombre, el mixin se ignora en vez de crashear.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "isSleepingInBed", at = @At("HEAD"), cancellable = true, require = 0)
    private void inmersivebackpack$allowBedroll(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
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
