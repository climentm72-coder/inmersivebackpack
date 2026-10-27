package net.climent.inmersivebackpack.client;

import net.climent.inmersivebackpack.item.SmartBackpackItem;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

public class BackpackFeatureRenderer<T extends LivingEntity, M extends BipedEntityModel<T>> extends FeatureRenderer<T, M> {
    private final ItemRenderer itemRenderer;

    public BackpackFeatureRenderer(FeatureRendererContext<T, M> context, ItemRenderer itemRenderer) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        // Obtenemos el item de la ranura 41 (o comprobamos si lleva la mochila equipada en la espalda)
        ItemStack backpackStack = ItemStack.EMPTY;
        
        // Si la entidad es un jugador, podemos leer el slot 41 de su inventario
        if (entity instanceof net.minecraft.entity.player.PlayerEntity player) {
            backpackStack = player.getInventory().getStack(41);
        }

        if (!backpackStack.isEmpty() && backpackStack.getItem() instanceof SmartBackpackItem) {
            matrices.push();
            
            // Posicionamos la mochila en la espalda del modelo bípedo (jugador)
            this.getContextModel().head.rotate(matrices); // O ajustamos al torso (body)
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));
            matrices.translate(0.0D, -0.45D, 0.15D);
            matrices.scale(0.65F, 0.65F, 0.65F);

            // Renderizamos el item en modo armadura/tercera persona en la espalda
            net.minecraft.client.MinecraftClient.getInstance().getItemRenderer().renderItem(
                entity,
                backpackStack,
                ModelTransformationMode.FIXED,
                false,
                matrices,
                vertexConsumers,
                entity.getWorld(),
                light,
                net.minecraft.client.render.entity.LivingEntityRenderer.getOverlay(entity, 0.0F),
                entity.getId()
            );
            
            matrices.pop();
        }
    }
}
