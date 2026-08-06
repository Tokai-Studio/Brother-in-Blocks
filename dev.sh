#!/usr/bin/env bash
# ============================================================
#  Brother in Blocks - Script de desarrollo
#
#  Usa el JDK 17 y las cachés de Gradle del disco Database
#  porque el disco del sistema no tiene espacio.
#
#  USO:
#    ./dev.sh build        -> compila el mod (crea el .jar)
#    ./dev.sh runClient    -> abre Minecraft con el mod para probarlo
#    ./dev.sh runServer    -> abre un servidor de prueba
#    ./dev.sh --version    -> comprueba que Gradle funciona
# ============================================================

set -e

export JAVA_HOME="/media/angel/Database/dev/jdk-17"
export GRADLE_USER_HOME="/media/angel/Database/dev/gradle-home"

cd "$(dirname "$0")"

echo "==> Java: $($JAVA_HOME/bin/java -version 2>&1 | head -1)"
echo "==> Caches de Gradle: $GRADLE_USER_HOME"

exec ./gradlew "$@"
