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

## 📋 Recursos verificados (licencias comprobadas en GitHub, 2026)

| Recurso | Licencia | ¿Lo usamos? | Para qué |
|---------|----------|-------------|----------|
| **Sistema Brain de vanilla** (aldeanos/zombis) | Mojang (gratis) | ✅ **Sí, base principal** | Decisiones y autonomía de la v2.0 |
| **PathNavigation de vanilla** | Mojang (gratis) | ✅ Ya lo usamos | Movimiento del Bro |
| **Taterzens** (`samolego/Taterzens`) | **MIT** | ✅ Copiar patrones | IA de NPCs servidor (es Fabric, pero el código es referencia válida) |
| **SmartBrainLib** (`Tslat/SmartBrainLib`) | **MPL-2.0** | ⏳ Dependencia cuando haga falta | API sobre el Brain para decisiones complejas |
| **GeckoLib** (`bernie-g/geckolib`) | **MIT** | ⏳ Futuro | Animaciones fluidas del Bro |
| **MCA (Comes Alive)** (`WildBamaBoy/...`) | **GPL-3.0** | 🚫 **NO copiar** | Su licencia "contagia": obligaría a publicar todo el mod como GPL |

> Verificado en los repositorios oficiales de GitHub (endpoint de licencia).

---

## ⚖️ Reglas de oro (léelas siempre antes de copiar código)

1. **MIT / Apache / BSD / MPL-2.0** → se puede copiar y adaptar, **manteniendo
   el aviso de copyright** del autor en el archivo o en un CREDITS.md.
2. **GPL / AGPL / LGPL** → **NO copiar** a menos que queramos publicar todo
   el mod bajo esa licencia (hoy el mod es *All Rights Reserved*).
3. **"All Rights Reserved"** (sin licencia) → **NO copiar**, es ilegal.
4. **Usar una biblioteca como dependencia** (ej. SmartBrainLib, GeckoLib)
   NO "contagia" tu mod: su licencia solo rige el código de esa biblioteca,
   no el tuyo. El jugador simplemente la instala aparte (o la incluimos).
5. **Vanilla (Mojang)** → siempre se puede usar como referencia; Mojang lo
   permite bajo sus términos del juego.

---

## 🗺️ Plan por etapas

| Etapa | Cuándo | Qué hacemos |
|-------|--------|-------------|
| **1. Vanilla + patrones** | AHORA (v1.0.0 → v2.0) | Goals de vanilla + copiar patrones de Taterzens y aldeanos. Cero dependencias, mod ligero |
| **2. SmartBrainLib** | Cuando la v2.0 de decisiones se complique (rasgos, memoria) | Añadir `Tslat/SmartBrainLib` (MPL-2.0) como dependencia de Forge 1.20.1 |
| **3. GeckoLib** | Cuando el Bro necesite animaciones de persona real | Añadir `bernie-g/geckolib` (MIT) |

---

## 📝 Notas

- Todo recurso copiado se anota en este documento y con su autor, para
  cumplir la atribución y poder publicar en Modrinth/CurseForge sin sustos.
- Antes de integrar una biblioteca nueva, se verifica su licencia en GitHub
  (como se hizo aquí) y su versión para Forge 1.20.1.
