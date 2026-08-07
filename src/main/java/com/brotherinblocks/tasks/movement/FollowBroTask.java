package com.brotherinblocks.tasks.movement;

import com.brotherinblocks.entity.BroEntity;
import com.brotherinblocks.tasksystem.Task;
import net.minecraft.world.entity.player.Player;

/**
 * TAREA: SEGUIR AL DUENO.
 * <p>
 * Traduce la logica del viejo FollowBroGoal al sistema de tareas:
 * el Bro camina hacia su dueno manteniendo una distancia prudente,
 * sin ser timido ni pegado.
 * <p>
 * - Se queda a ~3 bloques (cerquita, como un pana)
 * - Si te alejas mas de 6 bloques, te alcanza
 * - Si te alejas MUCHO, corre a alcanzarte (sin dudar)
 * - Recalcula la ruta cada 10 ticks (camina fluido, sin tirones)
 */
public class FollowBroTask extends Task {

    /** Distancia minima: si esta mas cerca que esto, se queda quieto */
    private static final double MIN_DISTANCE = 3.0D;
    /** Distancia maxima: si esta mas lejos que esto, empieza a caminar */
    private static final double MAX_DISTANCE = 6.0D;
    /** Multiplicador de velocidad cuando esta MUY lejos */
    private static final double RUN_SPEED_MULTIPLIER = 1.5D;
    /** Velocidad base a la que camina hacia el dueno */
    private static final double SPEED = 1.0D;
    /** Cada cuantos ticks recalcula la ruta (fluidez + rendimiento) */
    private static final int RECALC_INTERVAL = 10;

    /** Contador para no recalcular la ruta cada tick */
    private int timeToRecalcPath = 0;

    public FollowBroTask() {
    }

    @Override
    protected void onStart() {
        // Nada especial al arrancar
    }

    @Override
    protected Task onTick() {
        BroEntity bro = getBro();
        Player owner = getOwner(bro);
        if (bro == null || owner == null) {
            return null;
        }

        // Si el Bro esta peleando (lo gestiona el DefendBroGoal del
        // goalSelector, que seguira en el sistema viejo hasta migrarlo),
        // cedemos el control del movimiento al combate para que no se
        // peleen los dos sistemas.
        if (bro.getTarget() != null) {
            return null;
        }

        // Lo mira mientras camina (como un companero real)
        bro.getLookControl().setLookAt(owner, 10.0F, bro.getMaxHeadXRot());

        // Recalcula la ruta solo cada 10 ticks (0.5 segundos)
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = RECALC_INTERVAL;

            double distance = bro.distanceTo(owner);
            double speed = SPEED;

            // Si esta MUY lejos, corre a alcanzarlo (nada de timidez)
            if (distance > MAX_DISTANCE * 2) {
                speed = SPEED * RUN_SPEED_MULTIPLIER;
            }

            bro.getNavigation().moveTo(owner, speed);
        }
        return null;
    }

    @Override
    public boolean isFinished() {
        BroEntity bro = getBro();
        Player owner = getOwner(bro);
        // Termina cuando llega a distancia prudente
        return bro == null || owner == null || bro.distanceTo(owner) <= MIN_DISTANCE;
    }

    @Override
    protected void onStop(Task interruptTask) {
        BroEntity bro = getBro();
        if (bro != null) {
            // Se detiene (no sigue caminando a lo loco)
            bro.getNavigation().stop();
        }
    }

    @Override
    protected boolean isEqual(Task other) {
        // Todas las tareas de seguir son "la misma" (no reiniciar sin motivo)
        return other instanceof FollowBroTask;
    }

    @Override
    protected String toDebugString() {
        return "Seguir al dueno";
    }

    private Player getOwner(BroEntity bro) {
        if (bro == null || bro.getOwnerUUID() == null) {
            return null;
        }
        return bro.level().getPlayerByUUID(bro.getOwnerUUID());
    }
}
