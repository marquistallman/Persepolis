# Guía de Despliegue y Desarrollo - Persepolis Server

Este repositorio contiene la configuración automatizada para desplegar el servidor Persepolis de manera eficiente.

## Scripts de Instalación

El proyecto incluye scripts interactivos que gestionan la configuración de Git y las descargas de archivos según el entorno.

- **Windows:** `setup_server.ps1`
- **Linux/Mac:** `setup_server.sh`

## Menú de Gestión

Al ejecutar los scripts en el servidor, verás un menú con las siguientes opciones:

### 1. Recibir cambios de 'developer'
Actualiza el repositorio con los últimos cambios de la rama de desarrollo. Útil para mantenimientos menores o actualizaciones de scripts.

### 2. Instalar Sistema (LMMfunction y data)
Configura el entorno de producción. Activa el filtrado de carpetas (*Sparse Checkout*) para descargar únicamente el código fuente (`LMMfunction`) y los datos (`data`), eliminando cualquier otro archivo innecesario.

## Instrucciones Rápidas

### En Windows
1. Abre PowerShell en la carpeta del proyecto.
2. Ejecuta:
   ```powershell
   .\setup_server.ps1
   ```
3. Sigue las instrucciones en pantalla.

### En Linux (VPS)
1. Sube el archivo `setup_server.sh` al servidor.
2. Dale permisos de ejecución y ejecútalo:
   ```bash
   chmod +x setup_server.sh
   ./setup_server.sh
   ```