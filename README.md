# Guía de Despliegue y Desarrollo - Persepolis Server

Este repositorio contiene la configuración automatizada para desplegar el servidor Persepolis de manera eficiente.

## Scripts de Instalación

El proyecto incluye scripts interactivos que gestionan la configuración de Git y las descargas de archivos según el entorno.

 - **Windows:** `start_server.ps1`
 - **Linux/Mac:** `start_server.sh`

## Menú de Gestión

Al ejecutar los scripts en el servidor, verás un menú con las siguientes opciones:

### 1. Recibir cambios de 'developer'
Actualiza el repositorio con los últimos cambios de la rama de desarrollo. Útil para mantenimientos menores o actualizaciones de scripts.

### 2. Instalar Sistema (LMMfunction y data)
Configura el entorno de producción. Activa el filtrado de carpetas (*Sparse Checkout*) para descargar únicamente el código fuente (`LMMfunction`) y los datos (`data`), eliminando cualquier otro archivo innecesario.

## Control del Servidor (Producción)

El script `control_server.sh` permite gestionar el ciclo de vida del servidor utilizando `tmux` para mantener los procesos activos.

| Comando | Descripción |
| :--- | :--- |
| `./control_server.sh start` | Inicia el backend Java y el túnel Cloudflare en segundo plano. |
| `./control_server.sh stop` | Detiene la sesión y cierra los procesos. |
| `./control_server.sh restart` | Reinicia el servidor. |
| `./control_server.sh attach` | Muestra la consola en tiempo real. (Para salir sin detener: `Ctrl+B`, luego `D`). |

## Instrucciones Rápidas

### En Windows
1. Abre PowerShell en la carpeta del proyecto.
2. Ejecuta:
   ```powershell
   .\start_server.ps1
   ```
3. Sigue las instrucciones en pantalla.

### En Linux (VPS)
1. Sube el archivo `start_server.sh` al servidor.
2. Dale permisos de ejecución y ejecútalo:
   ```bash
   chmod +x start_server.sh
   ./start_server.sh
   ```
3. Una vez instalado, usa el script de control para iniciar el servicio:
   ```bash
   chmod +x control_server.sh
   ./control_server.sh start
   ```