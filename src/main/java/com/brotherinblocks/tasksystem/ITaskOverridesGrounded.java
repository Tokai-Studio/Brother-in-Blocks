package com.brotherinblocks.tasksystem;

/**
 * Marcador que permite a una tarea interrumpir a otras que requieren
 * estar en el suelo (ITaskRequiresGrounded).
 * <p>
 * Se usa en emergencias donde hay que actuar YA, aunque el Bro este
 * en el aire (p.ej. colocar un cubo de agua para no morir al caer).
 *
 * Reimplementado con codigo propio (arquitectura inspirada en ChatClef).
 */
public interface ITaskOverridesGrounded {
}
