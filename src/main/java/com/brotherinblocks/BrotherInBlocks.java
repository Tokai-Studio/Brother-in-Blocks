package com.brotherinblocks;

import com.brotherinblocks.entity.BroEntity;
import com.brotherinblocks.entity.BroModel;
import com.brotherinblocks.entity.BroRenderer;
import com.brotherinblocks.entity.ModEntities;
import com.brotherinblocks.event.BroChatHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Brother in Blocks
 *
 * Mod de Minecraft 1.20.1 (Forge) que simula jugar con un amigo/primo
 * en singleplayer. Esta es la version 0.2.0: el Bro aparece en el mundo.
 *
 * Roadmap:
 *  - v0.1.0  El mod carga y saluda (esqueleto)
 *  - v0.2.0  El Bro aparece en el mundo contigo (ESTAMOS AQUI)
 *  - v0.3.0  El Bro te sigue a distancia prudente
 *  - v0.4.0  El Bro tala madera y pica piedra
 *  - v0.5.0  El Bro te defiende de los monstruos
 *  - v1.0.0  Chat basico + pulido (MVP completo)
 */
@Mod(BrotherInBlocks.MOD_ID)
public class BrotherInBlocks {

    public static final String MOD_ID = "brotherinblocks";

    private static final Logger LOGGER = LogUtils.getLogger();

    public BrotherInBlocks() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Registra las entidades del mod en el bus de Forge
        ModEntities.ENTITY_TYPES.register(modEventBus);

        // Eventos de Forge (fase de modding)
        modEventBus.addListener(this::registerAttributes);
        modEventBus.addListener(this::registerLayerDefinitions);
        modEventBus.addListener(this::registerRenderers);

        // Eventos del juego
        MinecraftForge.EVENT_BUS.register(this);
        // Escucha el chat del jugador para responder al Bro
        MinecraftForge.EVENT_BUS.register(new BroChatHandler());

        LOGGER.info("=================================================");
        LOGGER.info("  Brother in Blocks ha despertado!");
        LOGGER.info("  Tu bro va a aparecer en el mundo contigo. GG!");
        LOGGER.info("=================================================");
    }

    /** Registra los atributos de la entidad (vida, velocidad, dano) */
    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.BRO.get(), BroEntity.createAttributes().build());
    }

    /** Registra el modelo 3D del Bro (solo cliente) */
    private void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BroModel.LAYER_LOCATION, BroModel::createBodyLayer);
    }

    /** Registra como se dibuja el Bro (solo cliente) */
    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BRO.get(), BroRenderer::new);
    }

    /**
     * Cuando el jugador entra al mundo, el Bro aparece a su lado.
     * Si el Bro ya existe en el mundo (partida guardada), no se duplica.
     */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide()) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        ServerPlayer serverPlayer = (ServerPlayer) player;

        // Si el Bro de ESTE jugador ya existe cerca, no creamos otro (evita duplicados)
        boolean yaExiste = level.getEntitiesOfClass(
                BroEntity.class,
                player.getBoundingBox().inflate(256.0D)).stream()
                .anyMatch(bro -> serverPlayer.getUUID().equals(bro.getOwnerUUID()));

        if (yaExiste) {
            LOGGER.info("Tu bro ya estaba aqui. Bienvenido de vuelta!");
            return;
        }

        // Busca una posicion segura (aire arriba, suelo debajo) a 2-3 bloques del jugador
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        double[] offsets = {2.0D, -2.0D, 3.0D, -3.0D};
        boolean encontroLugar = false;
        for (double d : offsets) {
            if (esLugarSeguro(level, x + d, y, z + d)) {
                x = x + d;
                z = z + d;
                encontroLugar = true;
                break;
            }
        }
        if (!encontroLugar) {
            LOGGER.info("No habia lugar seguro cerca, apareci un poco mas lejos.");
        }

        // Crea el Bro
        BroEntity bro = new BroEntity(ModEntities.BRO.get(), level);
        bro.setOwner(serverPlayer);
        bro.setPos(x, y, z);
        level.addFreshEntity(bro);

        LOGGER.info("Tu bro ha aparecido a tu lado. GG!");
    }

    /** Comprueba que el bloque es seguro: hay aire arriba y suelo debajo */
    private boolean esLugarSeguro(ServerLevel level, double x, double y, double z) {
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(
                (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        boolean aireArriba = level.getBlockState(pos.above()).isAir()
                && level.getBlockState(pos).isAir();
        boolean sueloDebajo = !level.getBlockState(pos.below()).isAir();
        return aireArriba && sueloDebajo;
    }
}
