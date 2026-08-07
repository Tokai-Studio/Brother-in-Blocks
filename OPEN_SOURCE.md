# 📚 Estrategia Open Source — Brother in Blocks

> **Decisión tomada (v1.0.0):** no reinventar la rueda. Para la IA, el
> movimiento y las decisiones del Bro, usamos **recursos open source
> verificados**: primero vanilla + patrones copiados de mods MIT, y
> añadimos bibliotecas (SmartBrainLib) solo cuando las mecánicas de la
> v2.0 se vuelvan complejas.

---

## ✅ La buena noticia sobre el "estancamiento"

El **movimiento/pathfinding ya está resuelto** desde v0.3.0: usamos el
`PathNavigation` de vanilla (el mismo sistema que usan los aldeanos y
los mods profesionales). **Nadie implementa pathfinding desde cero**;
eso sería reinventar la rueda. Lo que faltaba era el **cerebro de
decisiones** (para la v2.0), y para eso hay recursos listos.

---

## 📋 LISTA COMPLETA VERIFICADA (licencias comprobadas en GitHub, 2026)

### ✅ Categoría 1 — Cerebro / IA de decisiones (lo más importante para v2.0)

| Recurso | Repo (GitHub) | Licencia | ¿Qué nos da? | Estado |
|---------|---------------|----------|--------------|--------|
| **Sistema Brain de vanilla** (aldeanos/zombis) | Mojang (dentro del juego) | Gratis | Decisiones modulares con memoria: el patrón oficial de Mojang | ✅ **USAR AHORA** |
| **SmartBrainLib** | `Tslat/SmartBrainLib` | **MPL-2.0** | API lista sobre el Brain (behaviors, sensores) del creador de Advent of Ascension | ⏳ Etapa 2 (dependencia) |
| **CorgiLib** | `CorgiTaco-MC/CorgiLib` | **MPL-2.0** | Utilidades y goals para entidades | Opcional |

### ✅ Categoría 2 — Movimiento / Pathfinding

| Recurso | Repo (GitHub) | Licencia | ¿Qué nos da? | Estado |
|---------|---------------|----------|--------------|--------|
| **PathNavigation de vanilla** | Mojang | Gratis | Movimiento con colisiones, agua, escaleras | ✅ **YA LO USAMOS** |
| ~~PerViamInvenire~~ | `ldtteam/PerViamInvenire` | **GPL-3.0** 🚫 | Pathfinding ultra-rápido que abre puertas | ❌ **NO usar** (GPL contagia) |

### ✅ Categoría 3 — Animaciones y render

| Recurso | Repo (GitHub) | Licencia | ¿Qué nos da? | Estado |
|---------|---------------|----------|--------------|--------|
| **GeckoLib** | `bernie-g/geckolib` | **MIT** | Animaciones 3D fluidas de persona (Blockbench), usado por miles de mods | ⏳ Futuro (v3.0) |
| **Flywheel** | `Engine-Room/Flywheel` | **MIT** | Renderizado por GPU (instancing): FPS estable con muchas entidades | Opcional (solo si hay lag) |

### ✅ Categoría 4 — Referencias de código para copiar patrones

| Recurso | Repo (GitHub) | Licencia | ¿Qué copiamos de ahí? | Estado |
|---------|---------------|----------|------------------------|--------|
| **Taterzens** (NPCs) | `samolego/Taterzens` | **MIT** | IA de NPCs servidor: seguir, interactuar, inventario (es Fabric pero el código se porta) | ✅ **REFERENCIA** |
| **BuildingGadgets** | `Direwolf20-MC/BuildingGadgets` | **MIT** | Lógica de colocación de bloques en patrones: **la base para que el Bro construya su casa** | ✅ **REFERENCIA** |
| **Create** | `Creators-of-Create/Create` | **MIT** | Referencia de calidad y optimización de código | Referencia |

### ⏳ Opcional (solo si algún día portamos a Fabric o hacemos config)

| Recurso | Repo (GitHub) | Licencia | ¿Qué nos da? |
|---------|---------------|----------|--------------|
| **Architectury API** | `architectury/architectury-api` | **LGPL-3.0** | Escribir el mod una vez y compilar para Forge + Fabric |
| **Cloth Config** | `shedaniel/cloth-config` | **LGPL-3.0** | Menú de configuración bonito en el juego |

> LGPL es usable como dependencia (permite "enlazar" sin contagiar tu código).

---

### 🚫 NO USAR NUNCA (verificado — todos GPL o sin licencia)

| Recurso | Repo (GitHub) | Licencia | Por qué NO |
|---------|---------------|----------|------------|
| **ChatClef** (copiloto IA) | `elefant-ai/chatclef` | **AGPL-3.0** | La licencia **más estricta** que existe: ni siquiera permite usarlo en un servicio sin publicar TODO como AGPL. Solo LECTURA de arquitectura (sistema de tareas por cadenas). Clonado en `/media/angel/Database/chatclef` |
| **MCA (Comes Alive)** | `WildBamaBoy/minecraft-comes-alive` | **GPL-3.0** | Código "contagioso": obligaría a publicar todo el mod como GPL |
| **Minecolonies** | `ldtteam/minecolonies` | **GPL-3.0** | Ídem |
| **HumanCompanions** | `justinwon777/HumanCompanions` | **GPL-3.0** | Ídem (es el mod más parecido al nuestro, pero no se puede copiar) |
| **PerViamInvenire** | `ldtteam/PerViamInvenire` | **GPL-3.0** | Ídem (pathfinding) |
| **StructureHelper** | `ScalarVector1/StructureHelper` | **Sin licencia = ARR** | Sin permiso = ilegal |
| **Better Combat** | `ZsoltMolnarrr/BetterCombat` | **ARR** | Solo lectura, no copiar |
| **Moonlight Lib** | `MehVahdJukaar/Moonlight` | **Custom** | Términos especiales, no confiar |

> 🔍 **Dato curioso y clave:** los mods de compañeros humanos más famosos
> (MCA, HumanCompanions) son todos **GPL**, y las librerías de pathfinding
> avanzado también (PerViamInvenire). Por eso nuestra estrategia gana:
> **vanilla + patrones MIT/MPL** es el único camino que mantiene el mod
> 100% nuestro.

---

## ⚖️ Reglas de oro (léelas siempre antes de copiar código)

1. **MIT / Apache / BSD / MPL-2.0** → se puede copiar y adaptar, **manteniendo
   el aviso de copyright** del autor en el archivo o en un CREDITS.md.
2. **GPL / AGPL / LGPL** → **NO copiar** a menos que queramos publicar todo
   el mod bajo esa licencia (hoy el mod es *All Rights Reserved*).
3. **"All Rights Reserved"** (sin licencia) → **NO copiar**, es ilegal.
4. **Usar una biblioteca como dependencia** (ej. SmartBrainLib, GeckoLib,
   Cloth Config, Architectury) NO "contagia" tu mod: su licencia solo rige
   el código de esa biblioteca, no el tuyo. El jugador la instala aparte.
5. **Vanilla (Mojang)** → siempre se puede usar como referencia; Mojang lo
   permite bajo sus términos del juego.

---

## 🗺️ Plan por etapas

| Etapa | Cuándo | Qué hacemos |
|-------|--------|-------------|
| **1. Vanilla + patrones** | AHORA (v1.0.0 → v2.0) | Goals de vanilla + copiar patrones de Taterzens (MIT) y aldeanos. Cero dependencias, mod ligero |
| **2. SmartBrainLib** | Cuando la v2.0 de decisiones se complique (rasgos, memoria) | Añadir `Tslat/SmartBrainLib` (MPL-2.0) como dependencia de Forge 1.20.1 |
| **3. GeckoLib** | Cuando el Bro necesite animaciones de persona real | Añadir `bernie-g/geckolib` (MIT) |
| **4. Construcción** | Cuando el Bro se haga su casa | Copiar patrones de colocación de bloques de BuildingGadgets (MIT) |

---

## 📝 Notas

- Todo recurso copiado se anota en este documento y con su autor, para
  cumplir la atribución y poder publicar en Modrinth/CurseForge sin sustos.
- Antes de integrar una biblioteca nueva, se verifica su licencia en GitHub
  (como se hizo aquí) y su versión para Forge 1.20.1.
