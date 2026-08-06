package com.brotherinblocks.entity.ai;

import com.brotherinblocks.entity.BroEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/**
 * El Bro sigue a su dueno manteniendo una distancia prudente.
 *
 * v0.3.0:
 *  - Si estas lejos (> 10 bloques), camina hacia ti
 *  - Si te paras, el se detiene y te mira (no se te pega encima)
 *  - Nunca se acerca a menos de ~5 bloques (distancia prudente)
 */
public class FollowBroGoal extends Goal {

    private final BroEntity bro;
    /** Velocidad a la que camina hacia el dueno */
    private final double speedModifier;
    /** Distancia minima: si esta mas cerca que esto, se queda quieto */
    private final double minDistance;
    /** Distancia maxima: si esta mas lejos que esto, empieza a caminar */
    private final double maxDistance;

    public FollowBroGoal(BroEntity bro, double speedModifier, double minDistance, double maxDistance) {
        this.bro = bro;
        this.speedModifier = speedModifier;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        // Declaramos que usamos movimiento y mirada, para que otros goals
        // (combate, etc.) se coordinen con este sin conflictos
        this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /** Se activa cuando el dueno esta lejos (mas de maxDistance bloques) */
    @Override
    public boolean canUse() {
        Player owner = getOwner();
        if (owner == null || owner.isSpectator()) {
            return false;
        }
        // Solo si estamos en la misma dimension
        if (owner.level().dimension() != this.bro.level().dimension()) {
            return false;
        }
        return this.bro.distanceTo(owner) > this.maxDistance;
    }

    /** Sigue activo mientras el dueno siga a mas de minDistance bloques */
    @Override
    public boolean canContinueToUse() {
        Player owner = getOwner();
        if (owner == null || owner.isSpectator()) {
            return false;
        }
        if (owner.level().dimension() != this.bro.level().dimension()) {
            return false;
        }
        return this.bro.distanceTo(owner) > this.minDistance;
    }

    @Override
    public void tick() {
        Player owner = getOwner();
        if (owner == null) {
            return;
        }
        // Camina hacia el dueno
        this.bro.getNavigation().moveTo(owner, this.speedModifier);
        // Y lo mira mientras camina (como un companero real)
        this.bro.getLookControl().setLookAt(owner, 10.0F, this.bro.getMaxHeadXRot());
    }

    /** Cuando deja de perseguir, se detiene (no sigue caminando a lo loco) */
    @Override
    public void stop() {
        this.bro.getNavigation().stop();
    }

    private Player getOwner() {
        if (this.bro.getOwnerUUID() == null) {
            return null;
        }
        return this.bro.level().getPlayerByUUID(this.bro.getOwnerUUID());
    }
}
