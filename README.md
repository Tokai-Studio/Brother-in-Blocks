# 🧱 Brother in Blocks

> Tu amigo/primo virtual de Minecraft. Un compañero que juega contigo en
> singleplayer como si fuera multijugador.

**Versión:** 1.0.0 (MVP completo) · **Minecraft:** 1.20.1 · **Loader:** Forge (47.4.22)

## 🎯 ¿Qué es este proyecto?

Un mod para Minecraft que simula la experiencia de jugar con un amigo o primo
de verdad. No es un NPC obediente: tiene personalidad propia (estilo Sims 3),
toma decisiones, te propone planes por el chat, te hace bromas y recuerda lo
que pasa en la partida.

### Hoja de ruta — MVP (v1.0.0)

| Versión | Contenido | Detalle |
|---------|-----------|---------|
| **v0.1.0** | ✅ El mod carga y saluda | Esqueleto del mod |
| **v0.2.0** | ✅ El Bro aparece en el mundo | Con skin de jugador, no se pierde |
| **v0.3.0** | ✅ El Bro te sigue | Distancia prudente, espera cuando te paras |
| **v0.4.0** | ✅ El Bro trabaja contigo | Tala madera, pica piedra, recoge botín, sistema de hambre |
| **v0.5.0** | ✅ El Bro te defiende | Prioriza a tu atacante, se retira con poca vida |
| **v1.0.0** | ✅ El Bro te habla + MVP completo | Saludo, reacciones a muerte/logros, avisos de noche y creeper, anti-spam |

### Después del MVP

| Versión | Contenido |
|---------|-----------|
| **v2.0.0** | Decisiones propias, casa propia, inventario, reacciones |
| **v3.0.0** | Sistema de rasgos de personalidad (The Sims 3) |
| ... | Puerto a NeoForge, luego a Fabric |

> 🧠 **Estrategia de desarrollo:** usamos recursos open source verificados
> (vanilla + patrones MIT primero, SmartBrainLib/GeckoLib cuando hagan falta).
> Reglas y licencias en [`OPEN_SOURCE.md`](OPEN_SOURCE.md) y el plan
> profesional completo en [`ARQUITECTURA.md`](ARQUITECTURA.md).

## 🗂️ Estructura del proyecto

```
src/main/java/com/brotherinblocks/   → el código del mod
src/main/resources/                  → mods.toml (metadatos), pack.mcmeta
build.gradle                         → configuración de Gradle/Forge
gradle.properties                    → versión de Forge, nombre del mod, JDK
dev.sh                               → script para compilar/jugar (usa disco Database)
GUIA_PUBLICAR.md                     → guía para publicar el mod cuando esté listo
OPEN_SOURCE.md                       → estrategia y reglas de licencias (qué podemos copiar)
ARQUITECTURA.md                      → el plan profesional definitivo (IA Brain, roadmap maestro)
```

## 💾 IMPORTANTE: por qué todo lo pesado está en el disco "Database"

El disco del sistema **no tiene espacio**. Por eso:

- **JDK 17** → `/media/angel/Database/dev/jdk-17`
- **Cachés de Gradle** → `/media/angel/Database/dev/gradle-home`
- **Compilación (build)** → `/media/angel/Database/dev/build`
- **Mundo de prueba y logs** → `/media/angel/Database/dev/run`

En el proyecto solo queda el **código fuente** (pesa ~300 KB).

## 🚀 Cómo compilar y probar

Siempre usa el script `dev.sh` (configura Java y Gradle automáticamente):

```bash
# 1) Compilar el mod (la primera vez tarda: descarga Gradle, Forge y Minecraft)
./dev.sh build

# 2) Probar el mod abriendo Minecraft con el mod cargado
./dev.sh runClient
```

El `.jar` del mod queda en:
```
/media/angel/Database/dev/build/libs/brotherinblocks-0.1.0.jar
```

### Verificar que todo funciona

```bash
./dev.sh --version
```

## 🛠️ Requisitos

- El disco **Database** montado en `/media/angel/Database` (es donde vive el JDK)
- Nada más: el JDK 17 y Gradle ya están configurados dentro del proyecto

## 📝 Notas

- La licencia actual es **"All Rights Reserved"** (repo privado, en desarrollo).
  Cuando quieras publicarlo, mira la **GUIA_PUBLICAR.md**.
- Cuando el disco del sistema tenga espacio, se puede mover el JDK y las cachés
  a casa y quitar las líneas de `org.gradle.java.home` y `layout.buildDirectory`.
