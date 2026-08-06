# 🏗️ ARQUITECTURA PROFESIONAL — Brother in Blocks

> **El plan definitivo para llevar el mod de "MVP que funciona" a
> "otro nivel".** Basado en investigación real de cómo están hechos
> los mods más avanzados y de la IA de los mejores mods de
> compañeros/NPCs (verificado en sus repos y wikis, 2026).

---

## 1. 🔍 Diagnóstico: qué tenemos vs qué hace un mod profesional

| Área | Nosotros hoy (v1.0.0) | Mod profesional (Create, Alex's Mobs, Taterzens...) |
|------|----------------------|------------------------------------------------------|
| Registro de cosas | `DeferredRegister` ✅ (ya es el patrón correcto) | Ídem |
| IA del Bro | **Goals vanilla** (Follow, Gather, Defend) | **Sistema Brain** (el de los aldeanos) o SmartBrainLib |
| Datos | Todo en Java duro | **Data-driven** (JSON): frases, rasgos, comportamientos |
| Configuración | Ninguna | Menú de config (Cloth Config) |
| Red (multijugador) | Solo chat/UI básica | Paquetes de red profesionales |
| Texturas/modelos | 1 textura de Steve | Modelos Blockbench + animaciones GeckoLib |
| Rendimiento | Escaneos con cooldown ✅ (bien) | Cachés + sensores del Brain (mejor) |
| Estructura | Un solo loader (Forge) | Multi-loader (common/fabric/forge) o Architectury |

**Veredicto:** la base es sólida y ya usa patrones correctos. Lo que nos
separa de "otro nivel" es: **el Brain** (IA), **data-driven** (datos),
**memoria** (que recuerde), y **animaciones**.

---

## 2. 🧠 LA DECISIÓN CLAVE: el sistema BRAIN (no más goals sueltos)

Investigación del wiki oficial de SmartBrainLib — *"Brains vs Goals"*:

| | Goals (lo que usamos) | Brain (aldeanos / lo que viene) |
|---|---|---|
| **Potencia** | Un goal hace varias cosas → conflictos | Un behavior = una función, sin conflictos |
| **Prioridad** | Un solo número entero | Múltiples capas de prioridad |
| **Memoria** | Variables sueltas en la entidad | **Memorias** (MemoryModuleType): cualquier dato, cacheado |
| **Sensores** | Cada goal escanea entidades por su cuenta | **Un solo sensor** escanea y TODOS lo usan (más eficiente) |
| **Extensibilidad** | Cambiar algo = reescribir el goal | Mover/configurar behaviors sin tocar código |
| **Coste** | Ligero | Más CPU, pero con caché compite e incluso gana en IA compleja |

**El patrón de los aldeanos (1.20.1) que vamos a copiar:**
```
Brain<BroEntity>
 ├── Memoria: MemoryModuleType (última charla, hambre, casa, tarea, humor...)
 ├── Sensores: NearestPlayers, NearestLivingEntities, Daylight (escanean 1 vez)
 └── Behaviors: GateBehavior (condiciones) → OneShot / RunOne (actúan)
```

**Por qué importa para EL Bro:** sus rasgos de personalidad (Sims 3),
su memoria de eventos ("le robé sus cosas"), sus decisiones autónomas y
sus propuestas por chat son **exactamente** el tipo de IA compleja para
la que el Brain fue diseñado. Con goals sería un caos de conflictos.

**Plan:** v2.0 migra a Brain vanilla (patrón aldeano). Cuando se complique
con rasgos y memoria larga → SmartBrainLib (MPL-2.0) envuelve el mismo
sistema con una API limpia. No se tira nada: los goals actuales (seguir,
talar, defender) se convierten en behaviors uno a uno.

---

## 3. 🏆 LAS 3 MECÁNICAS "OTRO NIVEL" (de los mejores mods) — veredicto de la investigación

### 3.1 🧠 Memoria dinámica + penalización por repetición (de MCA)
El Bro **recuerda interacciones**: si le repites la misma broma o la misma
historia, te dice "ya me la contaste bro 😒" y pierde interés. Evita que el
diálogo se sienta robótico.

**Cómo lo hacemos:** `MemoryModuleType<Map<String, Long>>` guarda "tema →
última vez". Al repetir un tema → frase de rechazo + cooldown más largo.

### 3.2 📦 Cadena de necesidades y peticiones autónomas (de Minecolonies)
El Bro **reconoce sus carencias solo** y te pide lo que necesita:
- Herramienta rota → "bro, se me rompió el pico, me consigues otro?"
- Hambre → "tengo hambre, hay algo de comer?"
- Sin cama de noche → "¿me das un hueco en tu casa?"

**Cómo lo hacemos:** un behavior "needs" que evalúa cada 10s: hambre < X →
petición al jugador por chat (ya tenemos el sistema de chat y respuestas).

### 3.3 🎭 Personalidad + estado de ánimo (de MCA + nuestra idea Sims 3)
Rasgos (Temerario, Bromista, Perfeccionista, Cauteloso...) que **cambian
qué behaviors se activan y cómo**. Humor diario que altera las frases.

**Cómo lo hacemos:** rasgos en datos (JSON), guardados en la entidad. El
Brain consulta los rasgos en las condiciones de cada behavior. (¡Ya lo
teníamos en el roadmap, ahora sabemos CÓMO técnicamente!)

### Bonus 🏠 Construcción de su casa (de Minecolonies + BuildingGadgets MIT)
Patrón: el Bro usa un **esquema (schematic)** de casa simple, coloca
bloque a bloque visiblemente (como el constructor de Minecolonies), y
pide materiales cuando le faltan. La lógica de colocación de bloques se
copia del patrón de BuildingGadgets (MIT).

---

## 4. 🛠️ Prácticas profesionales a adoptar (orden de impacto)

1. **Brain system** → v2.0 (el corazón del mod)
2. **Data-driven en JSON** → frases y rasgos fuera del código Java
   (así cualquiera puede editar el mod sin recompilar; es lo que hacen
   los mods grandes)
3. **Memoria persistente** → guardar recuerdos en el NBT del Bro
   (ya guardamos inventario/hambre; se extiende a recuerdos y rasgos)
4. **Config (Cloth Config, LGPL)** → cantidad de madera por defecto,
   distancia de seguimiento, frecuencia de charla → ajustable por el jugador
5. **Animaciones (GeckoLib, MIT)** → el Bro camina, salta, mina y saluda
   con animaciones fluidas de Blockbench
6. **Rendimiento** → los sensores del Brain ya cachean; + Flywheel (MIT)
   solo si hay lag con muchos Bros
7. **Multi-loader (Taterzens/Architectury)** → SOLO cuando el mod esté
   maduro en Forge (no antes — duplica el trabajo)

---

## 5. 🗺️ ROADMAP MAESTRO (definitivo)

| Versión | Nombre | Contenido | Fuente del patrón |
|---------|--------|-----------|-------------------|
| **v1.0.0** | ✅ MVP completo | Aparece, sigue, trabaja, defiende, habla | (hecho) |
| **v2.0.0** | El Bro con criterio propio | Migración a **Brain**, decisiones autónomas, propuestas por chat, retomar órdenes | Aldeanos vanilla + SmartBrainLib wiki |
| **v2.1.0** | Necesidades | Peticiones autónomas: herramienta rota, hambre, cama de noche | Minecolonies |
| **v3.0.0** | Personalidad Sims 3 | Menú de creación del Bro, rasgos (JSON) que cambian behaviors, humor | MCA + nuestra idea original |
| **v3.1.0** | Memoria | Recuerda eventos: le robaste → se molesta; lo salvaste → te lo agradece. Penalización por repetir temas | MCA |
| **v4.0.0** | El Bro construye | Se hace su casa con esquema, pide materiales, la decora (si es Perfeccionista) | Minecolonies + BuildingGadgets |
| **v4.1.0** | Pulido de otro nivel | GeckoLib (animaciones), Cloth Config, más frases, logros conjuntos | GeckoLib + práctica general |
| **v5.0.0** | Multi-loader | Puerto a Fabric/NeoForge con Architectury | Taterzens |

---

## 6. 🧰 STACK FINAL recomendado (todo con licencia verificada)

| Componente | Recurso | Licencia | Cuándo |
|------------|---------|----------|--------|
| IA | Brain vanilla | Gratis | v2.0 |
| IA avanzada | SmartBrainLib | MPL-2.0 | v2.1+ si hace falta |
| Animaciones | GeckoLib | MIT | v4.1 |
| Config | Cloth Config | LGPL | v4.1 |
| Construcción | Patrón BuildingGadgets | MIT (patrón) | v4.0 |
| Rendimiento | Flywheel | MIT | solo si hay lag |
| Multi-loader | Architectury | LGPL | v5.0 |
| Referencias | Taterzens (MIT) · Minecolonies/MCA (GPL: solo leer) | — | siempre |

> ⚠️ Regla intacta de `OPEN_SOURCE.md`: de Minecolonies y MCA **solo se
> leen ideas**, nunca se copia código (GPL-3.0). Todo lo copiado es
> MIT/MPL con atribución.

---

## 7. ✅ Criterios de "otro nivel" (definición de hecho)

Un jugador dirá que el mod es de otro nivel cuando:
- [ ] El Bro **decide y propone** por su cuenta, no solo obedece
- [ ] El Bro **recuerda** lo que pasó hace días en la partida
- [ ] El Bro **tiene personalidad** única por mundo (rasgos)
- [ ] El Bro **trabaja y construye** de verdad (casa propia)
- [ ] El Bro **anima** como persona (GeckoLib) y **se configura** fácil
- [ ] El mod corre **fluido** (sin lag) incluso con el Bro trabajando
