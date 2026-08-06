package com.brotherinblocks.event;

import com.brotherinblocks.chat.ChatManager;
import com.brotherinblocks.entity.BroEntity;
import net.minecraft.advancements.Advancement;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

/**
 * Eventos que hacen hablar al Bro (v1.0.0).
 *
 *  - Cuando el JUGADOR muere, el Bro dice una frase de consuelo/burla
 *  - Cuando el jugador consigue un logro, el Bro lo felicita
 */
public class BroChatEvents {

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide) {
            return;
        }
        // Busca al Bro de este jugador y hace que reaccione
        List<BroEntity> bros = player.level().getEntitiesOfClass(BroEntity.class,
                player.getBoundingBox().inflate(128.0D));
        for (BroEntity bro : bros) {
            if (bro.getOwnerUUID() != null && bro.getOwnerUUID().equals(player.getUUID())) {
                bro.getChatManager().say(ChatManager.PLAYER_DEATHS);
                return; // solo un Bro reacciona (hay uno por jugador)
            }
        }
    }

    @SubscribeEvent
    public void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // Solo felicita por logros "de verdad": los de recetas no tienen
        // pantalla (display == null) y serian spam continuo al craftear
        Advancement advancement = event.getAdvancement();
        if (advancement == null || advancement.getDisplay() == null) {
            return;
        }
        List<BroEntity> bros = player.level().getEntitiesOfClass(BroEntity.class,
                player.getBoundingBox().inflate(128.0D));
        for (BroEntity bro : bros) {
            if (bro.getOwnerUUID() != null && bro.getOwnerUUID().equals(player.getUUID())) {
                bro.getChatManager().say(ChatManager.ACHIEVEMENTS);
                return;
            }
        }
    }
}
