package com.brotherinblocks.util.helpers;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Utilidad de depuracion del sistema de tareas.
 * <p>
 * En ChatClef existia una clase Debug que escribia a un archivo de log.
 * Aqui lo simplificamos: escribimos a la consola de Minecraft (log de
 * Forge), que es donde vemos los mensajes del mod al probarlo.
 */
public final class Debug {

    private static final Logger LOGGER = LogUtils.getLogger();

    private Debug() {
    }

    /** Mensaje interno del sistema de tareas (muy detallado) */
    public static void logInternal(String msg) {
        LOGGER.info("[Bro] " + msg);
    }

    /** Mensaje general de estado */
    public static void logMessage(String msg) {
        LOGGER.info("[Bro] " + msg);
    }

    /** Aviso (algo raro pero no fatal) */
    public static void logWarning(String msg) {
        LOGGER.warn("[Bro] " + msg);
    }
}
