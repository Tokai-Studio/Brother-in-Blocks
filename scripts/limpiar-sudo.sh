#!/bin/bash
# ============================================================
#  Limpieza de SISTEMA (requiere administrador) — Brother in Blocks
#  Ejecutar manualmente en la terminal:
#      bash ~/limpiar-sudo.sh
#  Te pedirá tu contraseña una vez.
# ============================================================

echo "[sudo] Pidiendo contraseña de administrador..."
sudo -v || { echo "❌ No se pudo autenticar."; exit 1; }

echo "[1/4] Limpiando caché de apt (paquetes descargados)..."
sudo apt-get clean

echo "[2/4] Comprimiendo logs del sistema (se mantienen ~50 MB)..."
sudo journalctl --vacuum-size=50M 2>/dev/null | tail -1

echo "[3/4] Eliminando kernels viejos y paquetes sin usar..."
sudo apt-get autoremove --purge -y 2>&1 | tail -2

echo "[4/4] Verificando..."
LIBRE=$(df -h / | awk 'NR==2{print $4}')
echo "✔ Limpieza de sistema completada. Espacio libre: $LIBRE"
echo ""
echo "💡 Consejo: revisa el peso de /var/log con: du -sh /var/log"
