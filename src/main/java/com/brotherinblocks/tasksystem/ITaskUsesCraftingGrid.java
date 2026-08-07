package com.brotherinblocks.tasksystem;

/**
 * Algunas tareas (sobre todo las que abren contenedores) se rompen si
 * la cuadricula de crafteo 2x2 no esta vacia.
 * <p>
 * Este marcador declara que la tarea necesita ciertos slots libres
 * antes de poder ejecutarse. En ChatClef era para el inventario del
 * jugador; aqui lo dejamos como parte de la arquitectura para cuando
 * el Bro manipule mesas de crafteo o contenedores.
 *
 * Reimplementado con codigo propio (arquitectura inspirada en ChatClef).
 */
public interface ITaskUsesCraftingGrid {
}
