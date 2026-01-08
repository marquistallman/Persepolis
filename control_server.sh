#!/bin/bash

# Script de control para Persepolis (Java + Cloudflare en Tmux)
# Permite iniciar/detener el servidor y el túnel manteniendo los procesos vivos.

# Directorio base (donde está este script)
BASE_DIR="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
cd "$BASE_DIR"

SESSION="persepolis"
JAR_FILE=$(find LMMfunction/target -name "*.jar" 2>/dev/null | head -n 1)

check_jar() {
    if [ -z "$JAR_FILE" ]; then
        echo "Error: No se encontró el archivo .jar en LMMfunction/target/"
        echo "Asegúrate de haber compilado el proyecto (opción 2 del menú de instalación)."
        exit 1
    fi
}

case "$1" in
    start)
        check_jar
        if tmux has-session -t $SESSION 2>/dev/null; then
            echo "El servidor ya está corriendo (Sesión tmux: $SESSION)."
        else
            echo "Iniciando servidor Persepolis..."
            
            # 1. Crear sesión y ventana para Java (Backend)
            tmux new-session -d -s $SESSION -n 'Backend'
            tmux send-keys -t $SESSION:0 "java -Xms512m -Xmx2g -XX:+UseG1GC -jar $JAR_FILE" C-m
            
            # 2. Crear ventana para Cloudflare Tunnel
            tmux new-window -t $SESSION:1 -n 'Tunnel'
            # Ejecuta el túnel. Si no está configurado, mostrará error en la ventana de tmux.
            tmux send-keys -t $SESSION:1 "cloudflared tunnel run" C-m
            
            echo "✅ Servidor iniciado en segundo plano."
            echo "👉 Usa 'control attach' para ver los logs y configurar el túnel si es necesario."
        fi
        ;;
    stop)
        if tmux has-session -t $SESSION 2>/dev/null; then
            tmux kill-session -t $SESSION
            echo "🛑 Servidor detenido."
        else
            echo "El servidor no está corriendo."
        fi
        ;;
    restart)
        $0 stop
        sleep 2
        $0 start
        ;;
    attach)
        if tmux has-session -t $SESSION 2>/dev/null; then
            echo "Conectando a la consola... (Presiona Ctrl+B, luego D para salir sin detener)"
            tmux attach -t $SESSION
        else
            echo "No hay sesión activa para adjuntar."
        fi
        ;;
    *)
        echo "Uso: control {start|stop|restart|attach}"
        exit 1
        ;;
esac