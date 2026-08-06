package com.brotherinblocks.event;

import com.brotherinblocks.entity.BroEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Escucha lo que el jugador escribe en el chat y se lo comunica a su Bro.
 *
 * v0.4.2:
 *  - Si el Bro pregunto \"quieres madera?\" y el jugador responde
 *    \"si 8\", \"si 4\", \"8\" -> arranca la orden con esa cantidad
 *  - Si responde \"no\", \"nope\" -> el Bro espera y vuelve a preguntar
 *  - Si no es una respuesta a su pregunta, el mensaje pasa normal
 */
public class BroChatHandler {

    /** Busca numeros en el mensaje (para la cantidad de madera) */
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null || player.level().isClientSide) {
            return;
        }

        String message = event.getMessage().getString().trim().toLowerCase();

        // Busca al Bro de este jugador que este esperando respuesta
        List<BroEntity> bros = player.level().getEntitiesOfClass(BroEntity.class,
                player.getBoundingBox().inflate(128.0D));
        for (BroEntity bro : bros) {
            // Solo respondemos al Bro de ESTE jugador y solo si esta esperando
            if (bro.isWaitingForAnswer()
                    && bro.getOwnerUUID() != null
                    && bro.getOwnerUUID().equals(player.getUUID())) {

                if (this.processAnswer(bro, message)) {
                    // La respuesta se consumio (el Bro la entendio)
                    event.setCanceled(true);
                }
                return;
            }
        }
    }

    /**
     * Interpreta la respuesta del jugador.
     * Devuelve true si el Bro la entendio (y no debe mostrarse en el chat).
     */
    private boolean processAnswer(BroEntity bro, String message) {
        // Respuesta NO
        if (message.equals("no") || message.startsWith("no ") || message.equals("nope")
                || message.equals("nop") || message.contains("no quiero")) {
            bro.refuseWoodOrder();
            return true;
        }

        // Respuesta SI (con o sin cantidad)
        boolean isYes = message.equals("si") || message.startsWith("si ")
                || message.equals("sí") || message.startsWith("sí ")
                || message.equals("dale") || message.equals("ok") || message.equals("okay")
                || message.equals("vamos") || message.equals("va")
                || message.matches("\\d+"); // solo un numero = si, con esa cantidad

        if (isYes) {
            int quantity = this.extractQuantity(message);
            bro.startWoodOrder(quantity);
            return true;
        }

        // No era una respuesta a la pregunta: el mensaje pasa normal
        return false;
    }

    /** Saca la cantidad de un mensaje tipo \"si 8\" o \"8\" (por defecto 8) */
    private int extractQuantity(String message) {
        Matcher matcher = NUMBER_PATTERN.matcher(message);
        if (matcher.find()) {
            try {
                int n = Integer.parseInt(matcher.group());
                return Math.max(1, Math.min(64, n)); // entre 1 y 64
            } catch (NumberFormatException ignored) {
                // si no se puede leer, usa el default
            }
        }
        return 8; // cantidad por defecto
    }
}
