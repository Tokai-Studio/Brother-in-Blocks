package com.brotherinblocks;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Brother in Blocks
 *
 * Mod de Minecraft 1.20.1 (Forge) que simula jugar con un amigo/primo
 * en singleplayer. Esta es la version 0.1.0: el esqueleto del mod.
 *
 * Roadmap:
 *  - v0.1.0  El mod carga y saluda (estamos aqui)
 *  - v0.2.0  El Bro aparece en el mundo y te sigue
 *  - v0.3.0  El Bro tala madera, pica piedra y te defiende
 *  - v0.4.0  Chat basico (mensajes al despertar, morir, encontrar algo)
 *  - v1.0.0  MVP completo de la version 1
 */
@Mod(BrotherInBlocks.MOD_ID)
public class BrotherInBlocks {

    public static final String MOD_ID = "brotherinblocks";

    private static final Logger LOGGER = LogUtils.getLogger();

    public BrotherInBlocks() {
        LOGGER.info("================================================");
        LOGGER.info("  Brother in Blocks v{} ha despertado!", "0.1.0");
        LOGGER.info("  Tu bro esta listo para jugar contigo. GG!");
        LOGGER.info("================================================");
    }
}
