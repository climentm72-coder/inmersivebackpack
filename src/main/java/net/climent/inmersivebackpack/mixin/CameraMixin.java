package net.climent.inmersivebackpack.mixin;

import net.climent.inmersivebackpack.client.SleepCinematicHandler;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private boolean thirdPerson;
    @Shadow private float pitch;

    @Inject(method = "update", at = @At("TAIL"))
    private void modifyCameraForSleep(BlockView area, Entity focusedEntity, boolean thirdPersonGui, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (SleepCinematicHandler.isCinematicActive() && focusedEntity != null) {
            // Fuerza la vista en tercera persona para que se vea al personaje acostado
            this.thirdPerson = true;
            // Ajusta el ángulo vertical (pitch) a -50 grados para mirar hacia arriba al cielo (sol/luna)
            this.pitch = -50.0f;
        }
    }
}
