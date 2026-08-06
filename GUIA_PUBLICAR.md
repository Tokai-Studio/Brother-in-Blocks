# 📦 GUÍA DE PUBLICACIÓN — Brother in Blocks

> Este documento te explica **qué hacer cuando quieras publicar tu mod**
> para que la gente lo pueda descargar. Sigue los pasos en orden.

---

## ⏳ ¿Cuándo publicar?

**Todavía NO.** Estás en desarrollo (v0.1.0). Publica cuando:

- ✅ El MVP (v1.0.0) funcione completo: Bro aparece, sigue, trabaja, defiende y habla
- ✅ Lo hayas probado varias horas sin errores
- ✅ El `.jar` se compile sin warnings importantes

---

## 🧭 Paso 1 — Decide la LICENCIA

Es lo más importante y **debes decidirlo ANTES** de publicar. Tu mod es tuyo
y solo tú decides qué pueden hacer otros con tu código:

| Licencia | Qué permite | Ideal si... |
|----------|-------------|-------------|
| **MIT** | Todos pueden usar, copiar y modificar, citándote | Quieres que crezca una comunidad alrededor |
| **LGPL** | Pueden usarlo pero si lo modifican deben compartir cambios | Quieres proteger tu trabajo pero que otros lo usen |
| **All Rights Reserved** | Nadie puede copiarlo sin permiso | Lo quieres solo para ti / por ahora |

> 📝 **Cómo se cambia en el proyecto:** edita `gradle.properties` →
> `mod_license=MIT` (o la que elijas). Ese texto aparece dentro del `.jar`.

---

## 🧭 Paso 2 — Prepara tu proyecto para el público

1. **Quita lo personal**: en `gradle.properties` revisa `mod_authors` y
   `mod_description` (que se vean profesionales).
2. **Sube la versión**: cambia `mod_version=0.1.0` → `1.0.0` antes de publicar.
3. **Crea el `.jar` final**:
   ```bash
   ./dev.sh build
   ```
   El archivo queda en `/media/angel/Database/dev/build/libs/`.

---

## 🧭 Paso 3 — Elige dónde publicar

| Plataforma | Ventaja | Costo |
|------------|---------|-------|
| **Modrinth** | Moderna, rápida, fácil de subir, buena para mods nuevos | Gratis |
| **CurseForge** | La más grande del mundo, muchísimo tráfico | Gratis |

> 💡 **Recomendación:** publica en **ambas**. Es gratis y llegas a más gente.
> Se necesita una cuenta en cada una (con tu email).

---

## 🧭 Paso 4 — Sube tu mod (CurseForge)

1. Crea cuenta en `curseforge.com` → "Create a Project"
2. Selecciona **Minecraft** → **Mods** → llena los datos:
   - **Name**: Brother in Blocks
   - **Summary**: "Tu amigo/primo virtual de Minecraft. Juega contigo en singleplayer como si fuera multijugador."
   - **License**: la que elegiste en el Paso 1
3. Sube el `.jar` y marca:
   - **Game version**: 1.20.1
   - **Mod loader**: Forge
4. Completa el perfil:
   - **Logo** (imagen 128x128 recomendado)
   - **Galería de imágenes** (capturas de tu mod funcionando)
   - **Descripción completa** (qué hace, cómo se usa, teclas, etc.)
5. Publica la primera versión

---

## 🧭 Paso 5 — Sube tu mod (Modrinth)

1. Crea cuenta en `modrinth.com` → "Dashboard" → "New project"
2. Llena los mismos datos (nombre, descripción, licencia)
3. Sube la versión y marca **1.20.1 + Forge**
4. Añade logo y capturas
5. Publica

---

## 🧭 Paso 6 — Después de publicar (importante)

- **Cada nueva versión del mod** = nueva subida (mismo proyecto, versión nueva)
- **Siempre sube también el código fuente** (tu repositorio de GitHub ya lo tiene)
- **Changelog**: escribe qué cambió en cada versión (ej: "v1.1: el Bro ahora cocina")
- Responde los comentarios de la comunidad (¡es lo que hace crecer un mod!)

---

## 🧭 Paso 7 — Bonus: tu repositorio de GitHub

Este repo puede ser público cuando publiques el mod (hoy es privado en desarrollo).
Si lo haces público, asegúrate de:

- ✅ El README esté bien (ya lo tienes)
- ✅ La licencia elegida esté escrita en el README
- ✅ No haya archivos con contraseñas o rutas personales
  (los archivos `dev.sh` y `scripts/` tienen rutas de TU pc — considera
  borrarlos del repo cuando lo hagas público, o reemplazar las rutas)

---

## ✅ Checklist final antes de publicar

```
[ ] MVP v1.0.0 completo y probado
[ ] Licencia decidida y escrita en gradle.properties
[ ] .jar compilado sin errores
[ ] Logo y capturas de pantalla listos
[ ] Cuentas creadas en CurseForge y Modrinth
[ ] Descripción y changelog escritos
[ ] Código fuente en GitHub
```

> 🎉 ¡Y listo! Ese es todo el proceso. Cuando llegues ahí, te acompaño paso a paso.
