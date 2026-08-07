package com.brotherinblocks.tasksystem;

import com.brotherinblocks.entity.BroEntity;
import com.brotherinblocks.util.helpers.Debug;

import java.util.ArrayList;
import java.util.List;

/**
 * EL ARBITRO DEL SISTEMA DE TAREAS.
 * <p>
 * Cada tick del juego, el TaskRunner mira todas las cadenas registradas
 * y ejecuta la que tiene MAYOR prioridad (la "necesidad mas urgente"
 * del Bro). Si cambia de cadena, avisa a la anterior para que se
 * detenga con orden (onInterrupt).
 * <p>
 * En ChatClef este sistema controlaba al jugador del cliente. Aqui lo
 * controlamos a una entidad: el {@link BroEntity}. Cada Bro tiene su
 * propio TaskRunner.
 *
 * Reimplementado con codigo propio (arquitectura inspirada en ChatClef).
 */
public class TaskRunner {

    /** Todas las cadenas de deseo registradas */
    private final List<TaskChain> chains = new ArrayList<>();

    /** El Bro que ejecuta este sistema */
    private final BroEntity bro;

    /** true si el sistema esta activo (se llama cada tick del Bro) */
    private boolean active;

    /** La cadena que se esta ejecutando (para detectar cambios) */
    private TaskChain cachedCurrentTaskChain = null;

    /** Reporte de estado para depuracion */
    public String statusReport = " (ninguna cadena ejecutandose) ";

    public TaskRunner(BroEntity bro) {
        this.bro = bro;
        active = false;
    }

    /**
     * Tick del sistema: elige la cadena de mayor prioridad y la ejecuta.
     * Lo llama el BroEntity en su aiStep (solo en el servidor).
     */
    public void tick() {
        if (!active) {
            statusReport = " (ninguna cadena ejecutandose) ";
            return;
        }

        // Busca la cadena activa con mayor prioridad
        TaskChain maxChain = null;
        float maxPriority = Float.NEGATIVE_INFINITY;
        for (TaskChain chain : chains) {
            if (!chain.isActive()) {
                continue;
            }
            float priority = chain.getPriority();
            if (priority > maxPriority) {
                maxPriority = priority;
                maxChain = chain;
            }
        }

        // Si cambiamos de cadena, avisamos a la anterior para que se detenga
        if (cachedCurrentTaskChain != null && maxChain != cachedCurrentTaskChain) {
            cachedCurrentTaskChain.onInterrupt(maxChain);
            Debug.logInternal("El cerebro cambio de deseo: "
                    + cachedCurrentTaskChain.getName() + " -> "
                    + (maxChain != null ? maxChain.getName() : "nada"));
        }
        cachedCurrentTaskChain = maxChain;

        if (maxChain != null) {
            statusReport = "Chain: " + maxChain.getName() + ", prioridad: " + maxPriority;
            maxChain.tick();
        } else {
            statusReport = " (ninguna cadena ejecutandose) ";
        }
    }

    /** Registra una cadena nueva (lo llama TaskChain al construirse) */
    public void addTaskChain(TaskChain chain) {
        chains.add(chain);
    }

    /** Activa el sistema (se llama cuando el Bro arranca su IA) */
    public void enable() {
        active = true;
    }

    /** Desactiva el sistema y detiene todas las cadenas */
    public void disable() {
        for (TaskChain chain : chains) {
            chain.stop();
        }
        // Limpia la referencia cacheada para no quedar con una cadena
        // obsoleta al re-activar el sistema
        cachedCurrentTaskChain = null;
        active = false;
        Debug.logMessage("TaskRunner detenido");
    }

    /** true si el sistema esta activo */
    public boolean isActive() {
        return active;
    }

    /** La cadena que se esta ejecutando ahora (o null) */
    public TaskChain getCurrentTaskChain() {
        return cachedCurrentTaskChain;
    }

    /** El Bro que ejecuta este sistema */
    public BroEntity getBro() {
        return bro;
    }
}
