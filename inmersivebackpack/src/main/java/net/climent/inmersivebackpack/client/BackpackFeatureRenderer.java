package net.climent.inmersivebackpack.client;

import net.climent.inmersivebackpack.InmersiveBackpackMod;
import net.climent.inmersivebackpack.network.WornInfo;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Dibuja la mochila (y el farol colgado) en la espalda de los jugadores. */
public class BackpackFeatureRenderer
        extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    private static final Identifier LEATHER_TEXTURE =
            new Identifier(InmersiveBackpackMod.MOD_ID, "textures/entity/backpack_leather.png");
    private static final Identifier RABBIT_TEXTURE =
            new Identifier(InmersiveBackpackMod.MOD_ID, "textures/entity/backpack_rabbit.png");

    private final BackpackModel model;

    public BackpackFeatureRenderer(
            FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context,
            EntityModelLoader loader) {
        super(context);
        this.model = new BackpackModel(loader.getModelPart(BackpackModel.LAYER));
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       AbstractClientPlayerEntity player, float limbAngle, float limbDistance, float tickDelta,
                       float animationProgress, float headYaw, float headPitch) {
        if (player.isInvisible() || player.isSleeping()) {
            return; // tumbado la mochila se ocultaria dentro del suelo
        }
        WornInfo info = ClientBackpackState.get(player.getUuid());
        if (info.variant() == 0) {
            return;
        }

        matrices.push();
        // Sigue al torso (se inclina al agacharse)
        this.getContextModel().body.rotate(matrices);

        Identifier texture = info.variant() == 2 ? RABBIT_TEXTURE : LEATHER_TEXTURE;
        model.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture)),
                light, OverlayTexture.DEFAULT_UV, info.bedroll(), info.lantern() != 0);

        if (info.lantern() != 0) {
            renderLantern(matrices, vertexConsumers, light, info.lantern() == 2);
        }
        matrices.pop();
    }

    /** Farol colgado de la cuerda: punto de enganche en (5.5, 12, 3.5) pixeles. */
    private void renderLantern(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, boolean soul) {
        BlockState state = (soul ? Blocks.SOUL_LANTERN : Blocks.LANTERN).getDefaultState()
                .with(LanternBlock.HANGING, true);

        matrices.push();
        matrices.translate(5.5f / 16f, 12f / 16f, 3.5f / 16f);
        float s = 0.4f;
        // El espacio del modelo de entidad esta invertido en x e y: se deshace con la escala negativa
        matrices.scale(-s, -s, s);
        // Pone el centro-arriba del bloque en el punto de enganche
        matrices.translate(-0.5f, -1.0f, -0.5f);
        MinecraftClient.getInstance().getBlockRenderManager()
                .renderBlockAsEntity(state, matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }
}
