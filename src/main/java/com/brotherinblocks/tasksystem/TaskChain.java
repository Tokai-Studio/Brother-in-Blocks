package com.brotherinblocks.tasksystem;

import java.util.ArrayList;
import java.util.List;

/**
 * UNA CADENA DE DESEO DEL BRO.
 * <p>
 * Cada cadena representa una "necesidad" o comportamiento del Bro
 * (defenderse, seguir, trabajar, comer...). En cada tick, el
 * {@link TaskRunner} elige la cadena activa con MAYOR prioridad y la
 * ejecuta, interrumpiendo a la anterior.
 * <p>
 * Una cadena no ejecuta logica de mundo directamente: delega en una
 * (o varias) {@link Task}.
 *
 * Reimplementado con codigo propio (arquitectura inspirada en ChatClef).
 */
public abstract class TaskChain {

    /** Las tareas ejecutadas en el ultimo tick (para depuracion) */
    private final List<Task> cachedTaskChain = new ArrayList<>();

    /** El runner al que pertenece esta cadena */
    private final TaskRunner runner;

    /**
     * @param runner el runner al que se auto-registra esta cadena
     */
    public TaskChain(TaskRunner runner) {
        this.runner = runner;
        runner.addTaskChain(this);
    }

    /** Tick de la cadena: limpia la cache de tareas y ejecuta su logica */
    public void tick() {
        cachedTaskChain.clear();
        onTick();
    }

    /** Detiene la cadena (se ejecuta al desactivar el sistema) */
    public void stop() {
        cachedTaskChain.clear();
        onStop();
    }

    /** Las tareas que se ejecutaron en el ultimo tick (depuracion) */
    public List<Task> getTasks() {
        return cachedTaskChain;
    }

    /** El runner de esta cadena */
    public TaskRunner getRunner() {
        return runner;
    }

    /** Registra una tarea como ejecutada en este tick (lo llama Task.tick) */
    void addTaskToChain(Task task) {
        cachedTaskChain.add(task);
    }

    // ---------- Metodos abstractos (los implementa cada cadena) ----------

    /** Logica de la cadena en cada tick */
    protected abstract void onTick();

    /** Limpieza al detener la cadena */
    protected abstract void onStop();

    /** Avisa a la cadena que otra de mayor prioridad la interrumpio */
    public abstract void onInterrupt(TaskChain other);

    /** Prioridad actual de este deseo (mas alto = se ejecuta primero) */
    public abstract float getPriority();

    /** true si este deseo esta activo ahora mismo */
    public abstract boolean isActive();

    /** Nombre corto de la cadena (para logs) */
    public abstract String getName();

    @Override
    public String toString() {
        return getName();
    }
}
