package com.brotherinblocks.tasksystem;

/**
 * CADENA DE TAREA UNICA (patron de ChatClef).
 * <p>
 * Es la base de casi todas las cadenas: la cadena tiene UNA tarea
 * principal a la vez ({@link #mainTask}). Cuando el deseo se activa,
 * la cadena fija la tarea con {@link #setTask(Task)} y esta se ejecuta
 * cada tick hasta que termina o es interrumpida.
 * <p>
 * Ejemplos de cadenas que la usaran: Seguir, Trabajar, Comer...
 *
 * Reimplementado con codigo propio (arquitectura inspirada en ChatClef).
 */
public abstract class SingleTaskChain extends TaskChain {

    /** La tarea principal que esta ejecutando esta cadena (o null) */
    protected Task mainTask = null;

    /** true si otra cadena de mayor prioridad nos interrumpio */
    private boolean interrupted = false;

    public SingleTaskChain(TaskRunner runner) {
        super(runner);
    }

    /**
     * Tick de la cadena: si hay tarea principal, la ejecuta; si termino
     * o la detuvieron, avisa a onTaskFinish para decidir que sigue.
     */
    @Override
    protected void onTick() {
        if (!isActive()) {
            return;
        }

        // Si nos interrumpieron y volvemos, la tarea arranca de nuevo
        if (interrupted) {
            interrupted = false;
            if (mainTask != null) {
                mainTask.reset();
            }
        }

        if (mainTask != null) {
            if (mainTask.isFinished() || mainTask.stopped()) {
                // La tarea termino: la cadena decide que hacer ahora
                onTaskFinish();
                mainTask = null;
            } else {
                mainTask.tick(this);
            }
        }
    }

    /** Detener la cadena: detiene tambien su tarea principal */
    @Override
    protected void onStop() {
        if (mainTask != null) {
            mainTask.stop();
            mainTask = null;
        }
    }

    /**
     * Cambia la tarea principal de la cadena.
     * Si la nueva es distinta a la actual, detiene la anterior.
     */
    public void setTask(Task task) {
        if (mainTask == null || !mainTask.equals(task)) {
            if (mainTask != null) {
                mainTask.stop(task);
            }
            mainTask = task;
            if (task != null) {
                task.reset();
            }
        }
    }

    /** La cadena esta activa mientras tenga una tarea que ejecutar */
    @Override
    public boolean isActive() {
        return mainTask != null;
    }

    /** La tarea principal actual (para inspeccion/debug) */
    public Task getCurrentTask() {
        return mainTask;
    }

    /** true si la tarea se esta ejecutando de verdad (sin interrupcion) */
    protected boolean isCurrentlyRunning() {
        return !interrupted && mainTask != null
                && mainTask.isActive() && !mainTask.isFinished();
    }

    /**
     * Otra cadena de mayor prioridad nos interrumpio: suspendemos la
     * tarea (sin destruirla) para poder retomarla al volver.
     */
    @Override
    public void onInterrupt(TaskChain other) {
        interrupted = true;
        if (mainTask != null && mainTask.isActive()) {
            mainTask.interrupt(null);
        }
    }

    /** La cadena decide que hacer cuando su tarea principal termina */
    protected abstract void onTaskFinish();
}
