package com.brotherinblocks.tasksystem;

/**
 * Permite que una tarea declare que su padre NO puede interrumpirla
 * en este momento.
 * <p>
 * Ejemplo real: si el Bro esta en medio de un salto para cruzar un
 * barranco, no queremos que una tarea de prioridad mas alta lo corte
 * a mitad del salto (se caeria a la lava).
 *
 * Reimplementado con codigo propio (arquitectura inspirada en ChatClef).
 */
public interface ITaskCanForce {

    /**
     * @param interruptingCandidate la tarea que intenta interrumpir la actual
     * @return true si la tarea actual debe seguir a la fuerza
     */
    boolean shouldForce(Task interruptingCandidate);
}
