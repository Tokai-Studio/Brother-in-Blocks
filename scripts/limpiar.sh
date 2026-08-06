#!/bin/bash
# ============================================================
#  Limpieza automática de usuario — Brother in Blocks
#  Se ejecuta al encender la PC (sin necesidad de contraseña)
#  Limpia solo cachés y temporales 100% seguros de borrar
# ============================================================

echo "[$(date '+%H:%M:%S')] 🧹 Iniciando limpieza de usuario..."

# 1) Cachés de usuario (las aplicaciones las regeneran solas)
rm -rf "$HOME/.cache"/* 2>/dev/null

# 2) Perfil temporal de Chrome (solo si Chrome NO está abierto)
if ! pgrep -x chrome >/dev/null 2>&1 && ! pgrep -x chromium >/dev/null 2>&1; then
    rm -rf "$HOME/.chromeprof-temp" 2>/dev/null
fi

# 3) Papelera del usuario
rm -rf "$HOME/.local/share/Trash"/* 2>/dev/null

# 4) Temporales de /tmp con más de 2 días
find /tmp -type f -mtime +2 -delete 2>/dev/null
find /tmp -type d -mtime +2 -empty -delete 2>/dev/null

# 5) Logs viejos de compilación del mod (en el disco Database)
rm -f /media/angel/Database/dev/*.log 2>/dev/null

LIBRE=$(df -h / | awk 'NR==2{print $4}')
echo "[$(date '+%H:%M:%S')] ✔ Limpieza completada. Espacio libre en el sistema: $LIBRE"
