package com.brotherinblocks.entity;

import com.brotherinblocks.BrotherInBlocks;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Modelo 3D del Bro: un cuerpo humanoide (como el de un jugador).
 * Se reutiliza el modelo base de HumanoidModel para tener la forma de jugador.
 */
public class BroModel extends HumanoidModel<BroEntity> {

    /** Ubicacion del modelo (se registra en EntityRenderersEvent.RegisterLayerDefinitions) */
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(BrotherInBlocks.MOD_ID, "bro"), "main");

    public BroModel(ModelPart root) {
        super(root);
    }

    /** Construye la forma del cuerpo (cabeza, torso, brazos, piernas) */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        return LayerDefinition.create(meshdefinition, 64, 64);
    }
}
