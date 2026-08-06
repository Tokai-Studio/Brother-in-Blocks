package com.brotherinblocks.entity;

import com.brotherinblocks.entity.ai.FollowBroGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * El Bro: tu companero que aparece en el mundo contigo.
 *
 * v0.2.0:
 *  - Aparece junto al jugador (lo genera BrotherInBlocks al entrar al mundo)
 *  - Tiene nombre propio visible sobre su cabeza
 *  - No recibe dano de caidas
 *  - No se ahoga
 *  - No desaparece solo (siempre queda guardado en el mundo)
 *  - No se pierde: si su dueno se aleja mucho, aparece cerca de el
 */
public class BroEntity extends PathfinderMob {

    /** Distancia maxima antes de teletransportarse hacia su dueno */
    private static final double MAX_FOLLOW_DISTANCE = 64.0D;

    /** UUID del jugador dueno (el que lo "recluto") */
    private UUID ownerUUID;

    public BroEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        // Nunca desaparece solo del mundo
        this.setPersistenceRequired();
        // Su nombre visible sobre la cabeza
        this.setCustomName(Component.literal("Bro"));
        this.setCustomNameVisible(true);
    }

    /** Atributos de la entidad: vida, velocidad, dano (se registran en EntityAttributeCreationEvent) */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                // 20 de vida = 10 corazones, igual que el jugador
                .add(Attributes.MAX_HEALTH, 20.0D)
                // Velocidad un poco menor que la del jugador corriendo, para
                // que camine a su ritmo y no se le pegue
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    /**
     * Los comportamientos (goals) del Bro.
     * v0.3.0: te sigue manteniendo distancia prudente y te mira.
     */
    @Override
    protected void registerGoals() {
        // Sigue al dueno: camina si esta a mas de 8 bloques, se detiene a los 4
        // (distancia prudente ~3-6 bloques, como en el diseno del MVP)
        this.goalSelector.addGoal(1, new FollowBroGoal(this, 0.8D, 4.0D, 8.0D));
        // Te mira cuando esta parado
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    /** Le decimos quien es su dueno al generarlo */
    public void setOwner(Player player) {
        this.ownerUUID = player.getUUID();
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Todo el estado que toca el mundo se maneja SOLO en el servidor
        if (!this.level().isClientSide) {
            // No se ahoga: si esta en agua, mantiene el aire lleno
            if (this.isInWater()) {
                this.setAirSupply(this.getMaxAirSupply());
            }

            // No se pierde: si el dueno esta muy lejos (y en la MISMA dimension),
            // aparece cerca de el. Si esta en otra dimension, no se mueve
            // (asi no se teletransporta a coordenadas de otra dimension aqui).
            if (this.ownerUUID != null) {
                Player owner = this.level().getPlayerByUUID(this.ownerUUID);
                if (owner != null
                        && owner.level().dimension() == this.level().dimension()
                        && this.distanceTo(owner) > MAX_FOLLOW_DISTANCE) {
                    // randomTeleport busca un lugar seguro (no dentro de bloques)
                    this.randomTeleport(owner.getX(), owner.getY(), owner.getZ(), true);
                }
            }
        }
    }

    /** Devuelve el UUID del jugador dueno (para evitar duplicados) */
    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }

    /** No recibe dano de caidas (para que no muera tontamente por un barranco) */
    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        return false;
    }

    /** Guarda quien es su dueno (se guarda con el mundo) */
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerUUID != null) {
            tag.putUUID("BroOwner", this.ownerUUID);
        }
    }

    /** Recupera quien es su dueno al cargar el mundo */
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("BroOwner")) {
            this.ownerUUID = tag.getUUID("BroOwner");
        }
    }
}
