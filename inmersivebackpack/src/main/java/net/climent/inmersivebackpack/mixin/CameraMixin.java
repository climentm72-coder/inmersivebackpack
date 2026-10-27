package net.climent.inmersivebackpack.mixin;

import net.climent.inmersivebackpack.client.SleepCinematicHandler;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
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
    private void modifyCameraForSleep(World area, Entity focusedEntity, boolean thirdPersonGui, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (SleepCinematicHandler.isCinematicActive() && focusedEntity != null) {
            this.thirdPerson = true;
            this.pitch = -50.0f;
        }
    }
}
