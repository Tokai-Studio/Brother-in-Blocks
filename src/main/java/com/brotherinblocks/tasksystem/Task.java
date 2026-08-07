package com.brotherinblocks.tasksystem;

import com.brotherinblocks.entity.BroEntity;
import com.brotherinblocks.util.helpers.Debug;

import java.util.function.Predicate;

/**
 * EL CORAZON DEL SISTEMA DE TAREAS.
 * <p>
 * Una tarea es una accion concreta con un ciclo de vida:
 * <ul>
 *   <li>{@link #onStart()} — se ejecuta UNA vez al comenzar la tarea</li>
 *   <li>{@link #onTick()} — se ejecuta cada tick del juego; puede
 *       devolver una SUB-TAREA (delegacion) o null si no hay mas</li>
 *   <li>{@link #onStop(Task)} — se ejecuta al terminar o ser interrumpida</li>
 * </ul>
 * <p>
 * La clave del diseno: las tareas pueden formar ARBOLES (una tarea
 * delega en otra mas pequena), y el sistema gestiona la interrupcion
 * de forma controlada: cuando una tarea quiere cambiar de sub-tarea,
 * primero pregunta si la actual se deja interrumpir (ITaskCanForce).
 *
 * Reimplementado con codigo propio (arquitectura inspirada en ChatClef).
 */
public abstract class Task {

    /** Estado de debug anterior (para no repetir mensajes) */
    private String oldDebugState = "";
    /** Estado de debug actual */
    private String debugState = "";

    /** Sub-tarea actual (la tarea hija en la que estamos delegando) */
    private Task sub = null;

    /** true = todavia no ha pasado por onStart en esta ejecucion */
    private boolean first = true;

    /** true = la tarea fue detenida */
    private boolean stopped = false;

    /** true = la tarea esta activa (en ejecucion) */
    private boolean active = false;

    /** La cadena que nos esta ejecutando (para llegar al Bro) */
    private TaskChain chain = null;

    /**
     * Tick del sistema: gestiona arranque, sub-tareas e interrupciones.
     * Lo llama la cadena (o la tarea padre) cada tick del juego.
     */
    public void tick(TaskChain parentChain) {
        // Nos apuntamos a la cadena que nos ejecuta (necesaria para
        // acceder al Bro desde cualquier punto del arbol)
        this.chain = parentChain;
        parentChain.addTaskToChain(this);

        // Primera vez: arranca la tarea
        if (first) {
            Debug.logInternal("Task START: " + this);
            active = true;
            onStart();
            first = false;
            stopped = false;
        }
        if (stopped) {
            return;
        }

        // La tarea decide: puede devolver una sub-tarea o null
        Task newSub = onTick();

        // Proteccion: si una tarea se devuelve a si misma como sub-tarea,
        // seria una recursion infinita (StackOverflow). Lo neutralizamos.
        if (newSub == this) {
            newSub = null;
        }

        // Debug: solo imprime cuando el estado cambia (anti-spam)
        if (!oldDebugState.equals(debugState)) {
            Debug.logInternal(toString());
            oldDebugState = debugState;
        }

        if (newSub != null) {
            // Tenemos sub-tarea nueva o la misma de antes.
            // Nota: sub puede ser null en la primera iteracion; por eso
            // comprobamos null ANTES de llamar a isEqual (que las subclases
            // no estan obligadas a proteger contra null).
            if (sub == null || !newSub.isEqual(sub)) {
                if (canBeInterrupted(sub, newSub)) {
                    // La sub-tarea anterior se detiene; entra la nueva
                    if (sub != null) {
                        sub.stop(newSub);
                    }
                    sub = newSub;
                }
            }
            // Ejecuta la sub-tarea (delegacion)
            sub.tick(parentChain);
        } else {
            // Ya no hay sub-tarea: detenemos la anterior si existia
            if (sub != null && canBeInterrupted(sub, null)) {
                sub.stop();
                sub = null;
            }
        }
    }

    /** Deja la tarea como nueva: la proxima ejecucion volvera a arrancar */
    public void reset() {
        first = true;
        active = false;
        stopped = false;
    }

    /** Detiene la tarea limpiamente */
    public void stop() {
        stop(null);
    }

    /**
     * Detiene la tarea. La proxima vez que se ejecute, correra onStart
     * de nuevo.
     *
     * @param interruptTask la tarea que la interrumpio (null = cierre limpio)
     */
    public void stop(Task interruptTask) {
        if (!active) {
            return;
        }
        Debug.logInternal("Task STOP: " + this + ", interrumpida por " + interruptTask);
        if (!first) {
            onStop(interruptTask);
        }
        // Detiene tambien la sub-tarea en cascada
        if (sub != null && !sub.stopped()) {
            sub.stop(interruptTask);
        }
        first = true;
        active = false;
        stopped = true;
    }

    /** Falla la tarea: la detiene y deja constancia del motivo */
    public void fail(String reason) {
        stop();
        Debug.logMessage("Task FAILED: " + reason);
    }

    /**
     * Suspende la tarea sin detenerla del todo (isActive sigue true).
     * Ejecuta onStop para liberar recursos, y al reanudarse correra
     * onStart otra vez.
     */
    public void interrupt(Task interruptTask) {
        if (!active) {
            return;
        }
        if (!first) {
            onStop(interruptTask);
        }
        if (sub != null && !sub.stopped()) {
            sub.interrupt(interruptTask);
        }
        first = true;
    }

    /** Estado de debug de la tarea (se muestra en los logs) */
    protected void setDebugState(String state) {
        debugState = (state == null) ? "" : state;
    }

    // ---------- Consultas de estado ----------

    /** true cuando la tarea termino su trabajo */
    public boolean isFinished() {
        return false;
    }

    /** true si la tarea esta en ejecucion ahora mismo */
    public boolean isActive() {
        return active;
    }

    /** true si la tarea fue detenida */
    public boolean stopped() {
        return stopped;
    }

    /** La cadena que nos esta ejecutando (null si no corremos) */
    public TaskChain getChain() {
        return chain;
    }

    /** El runner del sistema (para acceder a servicios globales) */
    public TaskRunner getRunner() {
        return (chain != null) ? chain.getRunner() : null;
    }

    /** El Bro que esta ejecutando este arbol de tareas (o null) */
    public BroEntity getBro() {
        TaskRunner runner = getRunner();
        return (runner != null) ? runner.getBro() : null;
    }

    // ---------- Metodos abstractos (los implementa cada tarea) ----------

    /** Se ejecuta una vez al comenzar la tarea */
    protected abstract void onStart();

    /**
     * Se ejecuta cada tick. Devuelve una sub-tarea para delegar,
     * o null si la tarea termino por este tick.
     */
    protected abstract Task onTick();

    /** Se ejecuta al detener/interrumpir la tarea */
    protected abstract void onStop(Task interruptTask);

    /**
     * true si dos tareas son "la misma" (para no reiniciar tareas iguales).
     * IMPORTANTE: puede recibir null (la primera vez), asi que la
     * implementacion debe devolver false si other == null.
     */
    protected abstract boolean isEqual(Task other);

    /** Texto corto que identifica la tarea en los logs */
    protected abstract String toDebugString();

    // ---------- Utilidades ----------

    @Override
    public String toString() {
        return "<" + toDebugString() + "> " + debugState;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Task task) && isEqual(task);
    }

    /**
     * AVISO: equals() usa isEqual(), que es una comparacion SEMANTICA
     * (dos tareas son iguales si hacen lo mismo, aunque sean objetos
     * distintos). Por eso NO se sobreescribe hashCode(): las tareas no
     * deben usarse en colecciones hash (HashSet/HashMap) porque romperian
     * el contrato equals/hashCode.
     */
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    /** true si esta tarea o alguna de sus sub-tareas cumple el predicado */
    public boolean thisOrChildSatisfies(Predicate<Task> pred) {
        Task t = this;
        while (t != null) {
            if (pred.test(t)) {
                return true;
            }
            t = t.sub;
        }
        return false;
    }

    /**
     * Decide si podemos cambiar de la sub-tarea actual a otra.
     * Una sub-tarea puede "negarse" a ser interrumpida si implementa
     * ITaskCanForce (p.ej. si el Bro esta a mitad de un salto).
     */
    private boolean canBeInterrupted(Task subTask, Task toInterruptWith) {
        if (subTask == null) {
            return true;
        }
        return subTask.thisOrChildSatisfies(task -> {
            if (task instanceof ITaskCanForce canForce) {
                // Si la sub-tarea pide fuerza, NO la podemos interrumpir
                return !canForce.shouldForce(toInterruptWith);
            }
            return true;
        });
    }
}
