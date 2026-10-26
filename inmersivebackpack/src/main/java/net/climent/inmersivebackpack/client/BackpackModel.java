package net.climent.inmersivebackpack.client;

import net.climent.inmersivebackpack.InmersiveBackpackMod;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Modelo 3D de la mochila, con cajas estilo vanilla (textura 64x64).
 * Coordenadas en pixeles relativas al torso del jugador: el torso va de x -4..4, y 0..12, z -2..2
 * y la ESPALDA es +z.
 */
public class BackpackModel {

    public static final EntityModelLayer LAYER =
            new EntityModelLayer(new Identifier(InmersiveBackpackMod.MOD_ID, "backpack"), "main");

    private final ModelPart root;
    private final ModelPart roll;
    private final ModelPart hook;

    public BackpackModel(ModelPart root) {
        this.root = root;
        this.roll = root.getChild("roll");
        this.hook = root.getChild("hook");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData r = data.getRoot();

        // Bolsa principal y solapa
        r.addChild("pouch", ModelPartBuilder.create().uv(0, 0).cuboid(-4f, 2f, 2.6f, 8f, 8f, 4f), ModelTransform.NONE);
        r.addChild("flap", ModelPartBuilder.create().uv(0, 12).cuboid(-4.5f, 1.5f, 2.1f, 9f, 3f, 5f), ModelTransform.NONE);
        // Correas delanteras y sobre los hombros
        r.addChild("strap_l", ModelPartBuilder.create().uv(0, 20).cuboid(-3f, 0f, -3.2f, 2f, 11f, 1f), ModelTransform.NONE);
        r.addChild("strap_r", ModelPartBuilder.create().uv(6, 20).cuboid(1f, 0f, -3.2f, 2f, 11f, 1f), ModelTransform.NONE);
        r.addChild("shoulder_l", ModelPartBuilder.create().uv(12, 20).cuboid(-3f, -1.4f, -3.2f, 2f, 1f, 6f), ModelTransform.NONE);
        r.addChild("shoulder_r", ModelPartBuilder.create().uv(28, 20).cuboid(1f, -1.4f, -3.2f, 2f, 1f, 6f), ModelTransform.NONE);
        // Manta enrollada bajo la bolsa (solo si hay manta equipada)
        r.addChild("roll", ModelPartBuilder.create().uv(0, 32).cuboid(-5f, 10f, 2.8f, 10f, 3f, 3f), ModelTransform.NONE);
        // Cuerda corta del farol (solo si hay farol)
        r.addChild("hook", ModelPartBuilder.create()
                .uv(26, 0).cuboid(4f, 8f, 3f, 2f, 1f, 1f)
                .uv(34, 0).cuboid(5f, 9f, 3f, 1f, 3f, 1f), ModelTransform.NONE);

        return TexturedModelData.of(data, 64, 64);
    }

    public void render(MatrixStack matrices, VertexConsumer consumer, int light, int overlay,
                       boolean showRoll, boolean showHook) {
        roll.visible = showRoll;
        hook.visible = showHook;
        root.render(matrices, consumer, light, overlay);
    }
}
