package net.climent.inmersivebackpack.client;

import net.climent.inmersivebackpack.item.SmartBackpackItem;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class BackpackFeatureRenderer<T extends LivingEntity, M extends BipedEntityModel<T>> extends FeatureRenderer<T, M> {
    private final ItemRenderer itemRenderer;

    public BackpackFeatureRenderer(FeatureRendererContext<T, M> context, ItemRenderer itemRenderer) {
        super(context);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity,
                      float limbAngle, float limbDistance, float tickDelta, float animationProgress,
                      float headYaw, float headPitch) {

        ItemStack backpackStack = ItemStack.EMPTY;

        if (entity instanceof PlayerEntity player) {
            backpackStack = player.getInventory().getStack(41);
        }

        if (backpackStack.isEmpty() || !(backpackStack.getItem() instanceof SmartBackpackItem)) {
            return;
        }

        matrices.push();
        matrices.translate(0.0D, -0.45D, 0.15D);
        matrices.scale(0.65F, 0.65F, 0.65F);

        itemRenderer.renderItem(
                entity,
                backpackStack,
                ModelTransformationMode.FIXED,
                false,
                matrices,
                vertexConsumers,
                entity.getWorld(),
                light,
                LivingEntityRenderer.getOverlay(entity, 0.0F),
                entity.getId()
        );

        matrices.pop();
    }
}
