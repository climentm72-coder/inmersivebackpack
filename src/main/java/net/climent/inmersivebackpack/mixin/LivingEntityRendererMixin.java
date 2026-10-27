package net.climent.inmersivebackpack.mixin;

import net.climent.inmersivebackpack.client.BackpackFeatureRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void addBackpackFeature(EntityRendererFactory.Context ctx, M model, float shadowRadius, CallbackInfo ci) {
        LivingEntityRenderer<T, M> renderer = (LivingEntityRenderer<T, M>) (Object) this;
        renderer.addFeature(new BackpackFeatureRenderer<>(renderer, ctx.getItemRenderer()));
    }
}
