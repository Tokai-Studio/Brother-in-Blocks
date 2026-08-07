package com.brotherinblocks.chains;

import com.brotherinblocks.entity.BroEntity;
import com.brotherinblocks.tasks.movement.FollowBroTask;
import com.brotherinblocks.tasksystem.SingleTaskChain;
import com.brotherinblocks.tasksystem.Task;
import com.brotherinblocks.tasksystem.TaskChain;
import com.brotherinblocks.tasksystem.TaskRunner;
import net.minecraft.world.entity.player.Player;

/**
 * CADENA: SEGUIR AL DUENO.
 * <p>
 * El deseo de "estar cerca del dueno". Esta cadena esta SIEMPRE
 * pendiente (como la cadena de defensa de ChatClef): en cada tick
 * calcula su prioridad segun la distancia.
 * <p>
 * Prioridad: 10 si el dueno esta lejos (nos toca alcanzarlo),
 * -infinito si estamos bien (no es un deseo urgente).
 */
public class FollowChain extends SingleTaskChain {

    /** Prioridad cuando hay que seguir (media-baja, tras defensa/comida) */
    private static final float FOLLOW_PRIORITY = 10.0F;
    /** Distancia maxima: si el dueno esta mas lejos, queremos seguirlo */
    private static final double MAX_DISTANCE = 6.0D;

    /** Ultima prioridad calculada (para no recalcularla dos veces por tick) */
    private float cachedLastPriority = Float.NEGATIVE_INFINITY;

    public FollowChain(TaskRunner runner) {
        super(runner);
    }

    /**
     * Siempre estamos evaluando si hay que seguir al dueno.
     * (Igual que MobDefenseChain en ChatClef: "we're always checking".)
     */
    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public float getPriority() {
        cachedLastPriority = getPriorityInner();
        // Si no hay tarea que ejecutar, no competimos por el tick
        if (getCurrentTask() == null && cachedLastPriority > 0) {
            cachedLastPriority = 0;
        }
        return cachedLastPriority;
    }

    /** Calcula la prioridad real del deseo de seguir */
    private float getPriorityInner() {
        Player owner = getOwner();
        if (owner == null || owner.isSpectator()) {
            return Float.NEGATIVE_INFINITY;
        }
        // Solo si estamos en la misma dimension
        BroEntity bro = getRunner().getBro();
        if (bro == null || owner.level().dimension() != bro.level().dimension()) {
            return Float.NEGATIVE_INFINITY;
        }
        // Hay que seguir solo si el dueno se alejo
        if (bro.distanceTo(owner) > MAX_DISTANCE) {
            return FOLLOW_PRIORITY;
        }
        return Float.NEGATIVE_INFINITY;
    }

    @Override
    protected void onTick() {
        // Si no hay deseo de seguir (dueno cerca), no hacemos nada
        if (cachedLastPriority == Float.NEGATIVE_INFINITY) {
            return;
        }
        // Nos aseguramos de tener la tarea de seguir lista
        Task current = getCurrentTask();
        if (!(current instanceof FollowBroTask)) {
            setTask(new FollowBroTask());
        }
        // Ejecuta la tarea principal (patron de SingleTaskChain)
        super.onTick();
    }

    @Override
    protected void onTaskFinish() {
        // Al llegar junto al dueno no hay nada especial que hacer:
        // la proxima vez getPriority devolvera -infinito y nos calmamos.
    }

    @Override
    public void onInterrupt(TaskChain other) {
        // Otra cadena mas urgente (defensa, comida) nos gano: suspendemos
        super.onInterrupt(other);
    }

    @Override
    public String getName() {
        return "Seguir";
    }

    private Player getOwner() {
        BroEntity bro = getRunner().getBro();
        if (bro == null || bro.getOwnerUUID() == null) {
            return null;
        }
        return bro.level().getPlayerByUUID(bro.getOwnerUUID());
    }
}
