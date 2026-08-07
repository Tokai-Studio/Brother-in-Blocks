package com.brotherinblocks.tasksystem;

import com.brotherinblocks.entity.BroEntity;

/**
 * Algunas tareas se rompen si las interrumpimos en el aire.
 * <p>
 * Ejemplo: si el Bro esta saltando (parkour) y alguien lo interrumpe,
 * caeria al vacio. Esta interfaz obliga a esperar a que el Bro toque
 * el suelo antes de permitir la interrupcion.
 * <p>
 * A diferencia de ChatClef (que miraba al jugador del cliente), aqui
 * miramos a NUESTRA entidad: el BroEntity.
 *
 * Reimplementado con codigo propio (arquitectura inspirada en ChatClef).
 */
public interface ITaskRequiresGrounded extends ITaskCanForce {

    @Override
    default boolean shouldForce(Task interruptingCandidate) {
        // Si la tarea que llega pide interrumpir "a la fuerza" el suelo
        // (p.ej. un rescate urgente), dejamos pasar.
        if (interruptingCandidate instanceof ITaskOverridesGrounded) {
            return false;
        }

        // Esta interfaz siempre la implementa una Task; de ella sacamos
        // el Bro via su TaskRunner.
        if (this instanceof Task task) {
            BroEntity bro = task.getBro();
            if (bro == null) {
                return false;
            }
            // Forzamos la continuidad si NO estamos firmes en el suelo.
            // Nota: nombres oficiales de Mojang en 1.20.1 (onGround(),
            // isInWater(), isSwimming()).
            boolean grounded = bro.onGround() || bro.isInWater()
                    || bro.isSwimming();
            return !grounded;
        }
        return false;
    }
}
