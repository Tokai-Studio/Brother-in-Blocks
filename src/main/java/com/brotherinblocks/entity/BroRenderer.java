package com.brotherinblocks.entity;

import com.brotherinblocks.BrotherInBlocks;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Como se dibuja el Bro en pantalla.
 *
 * v0.2.0: usa una textura de jugador (Steve) copiada DENTRO del mod,
 * para que se vea como un jugador real (skin de jugador).
 */
public class BroRenderer extends HumanoidMobRenderer<BroEntity, BroModel> {

    /** Textura del Bro (vive dentro de nuestro mod) */
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BrotherInBlocks.MOD_ID, "textures/entity/bro.png");

    public BroRenderer(EntityRendererProvider.Context context) {
        super(context, new BroModel(context.bakeLayer(BroModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(BroEntity entity) {
        return TEXTURE;
    }
}
