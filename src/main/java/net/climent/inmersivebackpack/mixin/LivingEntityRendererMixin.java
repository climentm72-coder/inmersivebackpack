package net.climent.inmersivebackpack.mixin;

import net.climent.inmersivebackpack.client.BackpackFeatureRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.entity.LivingEntity; // o net.minecraft.entity.LivingEntity
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends net.minecraft.entity.LivingEntity, M extends EntityModel<T>> {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void addBackpackFeature(net.minecraft.client.render.entity.EntityRendererFactory.Context ctx, M model, float shadowRadius, CallbackInfo ci) {
        LivingEntityRenderer<?, ?> renderer = (LivingEntityRenderer<?, ?>) (Object) this;
        if (renderer instanceof PlayerEntityRenderer playerRenderer) {
            playerRenderer.addFeature(new BackpackFeatureRenderer(
                playerRenderer,
                ctx.getItemRenderer()
            ));
        }
    }
}
