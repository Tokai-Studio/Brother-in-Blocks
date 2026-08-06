package com.brotherinblocks.entity.ai;

import com.brotherinblocks.entity.BroEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * El Bro te defiende de los monstruos (v0.5.0).
 *
 *  - Detecta monstruos hostiles cerca de ti (radio 16 bloques)
 *  - PRIORIZA a quien te esta atacando a ti o al Bro
 *  - Pelea de verdad: te persigue y golpea (dano 4, como un jugador sin espada)
 *  - Con poca vida (menos de 6) se retira: deja de pelear y vuelve contigo
 */
public class DefendBroGoal extends Goal {

    /** Radio en el que detecta peligro alrededor del dueno */
    private static final int SEARCH_RADIUS = 16;
    /** Distancia a la que puede golpear (como un jugador) */
    private static final double ATTACK_REACH = 2.5D;
    /** Cada cuantos ticks golpea (20 ticks = 1 golpe por segundo) */
    private static final int ATTACK_INTERVAL = 20;
    /** Con menos vida que esto se retira (30% de los 20 de vida) */
    private static final double HEALTH_TO_RETREAT = 6.0D;
    /** Velocidad a la que persigue al enemigo (1.2 = mas rapido que caminar) */
    private static final double FIGHT_SPEED = 1.2D;
    /** Cada cuantos ticks reescanea buscando enemigos (0.5s) */
    private static final int SCAN_INTERVAL = 40;
    /** Si pierde de vista al enemigo, cuantos ticks lo sigue antes de soltarlo */
    private static final int LOST_SIGHT_LIMIT = 100;

    private final BroEntity bro;
    /** El enemigo al que esta atacando ahora mismo */
    private LivingEntity target;
    /** Contador entre golpes */
    private int ticksUntilNextAttack;
    /** Contador para no recalcular la ruta cada tick */
    private int timeToRecalcPath;
    /** Para no repetir el mensaje de retirada varias veces seguidas */
    private boolean retreatSaid = false;
    /** Contador para no reescannear enemigos cada tick (rendimiento) */
    private int scanCooldown = 0;
    /** Cuantos ticks lleva sin ver al enemigo (para no perseguirlo por paredes) */
    private int lostSightTicks = 0;

    public DefendBroGoal(BroEntity bro) {
        this.bro = bro;
        // Necesita moverse, mirar y fijar objetivo (prioridad alta: gana a seguir/trabajar)
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.TARGET));
    }

    /** Se activa si hay un enemigo cerca del dueno (de dia o de noche) */
    @Override
    public boolean canUse() {
        Player owner = getOwner();
        if (owner == null || owner.isSpectator()) {
            return false;
        }
        // Solo en la misma dimension
        if (owner.level().dimension() != this.bro.level().dimension()) {
            return false;
        }
        // Con poca vida no pelea: se retira
        if (this.bro.getHealth() < HEALTH_TO_RETREAT) {
            return false;
        }
        // Solo si el dueno esta cerca (el Bro no va a buscar pelea muy lejos)
        if (this.bro.distanceTo(owner) > SEARCH_RADIUS) {
            return false;
        }
        // Reescanea los enemigos solo cada SCAN_INTERVAL ticks (rendimiento)
        if (this.scanCooldown-- > 0) {
            return false;
        }
        this.scanCooldown = SCAN_INTERVAL;
        this.target = this.findTarget(owner);
        return this.target != null;
    }

    /** Sigue peleando mientras el objetivo siga vivo y cerca */
    @Override
    public boolean canContinueToUse() {
        if (this.target == null || this.target.isDeadOrDying()) {
            return false;
        }
        Player owner = getOwner();
        if (owner == null || owner.isSpectator()) {
            return false;
        }
        // Con poca vida se retira (una sola vez avisa al jugador)
        if (this.bro.getHealth() < HEALTH_TO_RETREAT) {
            if (!this.retreatSaid) {
                this.retreatSaid = true;
                this.bro.getChatManager().say(java.util.List.of(
                        "Bro, estoy muy mal, me retiro.",
                        "Me repliego bro, cura el resto tu."));
            }
            return false;
        }
        // Si el dueno se alejo mucho, deja de pelear y lo sigue
        if (this.bro.distanceTo(owner) > SEARCH_RADIUS * 1.5D) {
            return false;
        }
        // No persigue al enemigo hasta el fin del mundo: si huye muy lejos, lo suelta
        if (this.bro.distanceTo(this.target) > SEARCH_RADIUS * 2.0D) {
            return false;
        }
        // No persigue a traves de paredes: si pierde de vista al enemigo
        // mucho rato, lo suelta (no va a morir en la lava persiguiendolo)
        if (this.bro.getSensing().hasLineOfSight(this.target)) {
            this.lostSightTicks = 0;
        } else if (++this.lostSightTicks > LOST_SIGHT_LIMIT) {
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        this.ticksUntilNextAttack = 0;
        this.timeToRecalcPath = 0;
        this.retreatSaid = false;
        this.lostSightTicks = 0;
        this.scanCooldown = 0;
        this.bro.setTarget(this.target);
    }

    @Override
    public void stop() {
        this.bro.setTarget(null);
        this.target = null;
        this.bro.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.target == null) {
            return;
        }

        // Lo mira mientras pelea (como un jugador de verdad)
        this.bro.getLookControl().setLookAt(this.target, 30.0F, 30.0F);

        double distSqr = this.bro.distanceToSqr(this.target);

        // Si esta a distancia de golpe y ve al enemigo: ataca
        if (this.ticksUntilNextAttack > 0) {
            this.ticksUntilNextAttack--;
        }
        if (distSqr <= ATTACK_REACH * ATTACK_REACH
                && this.bro.getSensing().hasLineOfSight(this.target)
                && this.ticksUntilNextAttack == 0) {
            this.ticksUntilNextAttack = ATTACK_INTERVAL;
            this.bro.swing(InteractionHand.MAIN_HAND);
            this.bro.doHurtTarget(this.target);
            return;
        }

        // Sino, lo persigue (recalculando la ruta cada 10 ticks)
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;
            this.bro.getNavigation().moveTo(this.target, FIGHT_SPEED);
        }
    }

    /**
     * Busca al enemigo. Prioridad:
     *  1) Quien este atacando al dueno o al Bro (aunque no sea un monstruo)
     *  2) El monstruo hostil mas cercano
     */
    private LivingEntity findTarget(Player owner) {
        AABB area = owner.getBoundingBox().inflate(SEARCH_RADIUS);
        List<Mob> mobs = this.bro.level().getEntitiesOfClass(Mob.class, area);

        // 1) Alguien atacando al dueno o al Bro: es el peligro numero 1
        for (Mob mob : mobs) {
            if (mob.isDeadOrDying() || mob == this.bro) {
                continue;
            }
            if (mob.getTarget() == owner || mob.getLastHurtByMob() == owner
                    || mob.getTarget() == this.bro || mob.getLastHurtByMob() == this.bro) {
                return mob;
            }
        }

        // 2) El monstruo hostil mas cercano (zombies, esqueletos, creepers...)
        Mob nearest = null;
        double bestDistance = Double.MAX_VALUE;
        for (Mob mob : mobs) {
            if (mob.isDeadOrDying() || mob == this.bro) {
                continue;
            }
            if (!(mob instanceof Enemy)) {
                continue;
            }
            double distance = mob.distanceToSqr(owner);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = mob;
            }
        }
        return nearest;
    }

    private Player getOwner() {
        if (this.bro.getOwnerUUID() == null) {
            return null;
        }
        return this.bro.level().getPlayerByUUID(this.bro.getOwnerUUID());
    }
}
