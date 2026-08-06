package com.brotherinblocks.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Como se dibuja el Bro en pantalla.
 *
 * v0.2.0: usa la textura de jugador de Steve del propio Minecraft,
 * para que se vea como un jugador real (skin de jugador).
 *
 * Mas adelante (v0.3+): se puede poner una textura propia o usar la
 * skin del jugador dueno.
 */
public class BroRenderer extends HumanoidMobRenderer<BroEntity, BroModel> {

    /** Textura de jugador clasico de Minecraft (Steve) */
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft", "textures/entity/steve.png");

    public BroRenderer(EntityRendererProvider.Context context) {
        super(context, new BroModel(context.bakeLayer(BroModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(BroEntity entity) {
        return TEXTURE;
    }
}
