# Persepolis IA

Este proyecto es el backend del sistema, desarrollado con **Spring Boot** y **Java 21**. Se encarga de la lógica de IA, la base de datos y de servir la interfaz de usuario (Frontend).

## 📋 Requisitos Previos

Para trabajar en este proyecto necesitas instalar:

1.  **Java 21 (JDK)**: El motor del backend.
2.  **Maven**: Para gestionar las librerías y compilar.
3.  **Git Bash** (si usas Windows): Para ejecutar los scripts de automatización.
4.  **(Opcional) Node.js**: Solo si necesitas modificar el diseño (CSS/Tailwind).

## ⚡ Panel de Control (Windows)

Para facilitar todo, hemos creado un menú interactivo. Si usas Windows, solo necesitas hacer esto:

1.  Haz clic derecho en el archivo `menu.ps1`.
2.  Selecciona **"Ejecutar con PowerShell"**.

¡Desde ahí podrás iniciar el servidor, guardar cambios o actualizar el diseño sin escribir comandos!

## 🚀 Puesta en Marcha

### 1. Configuración Inicial
Clona el repositorio y asegúrate de estar en la carpeta `LMMfunction`.

Verifica el archivo `src/main/resources/application.properties`. Si tienes una clave de OpenRouter, colócala ahí:
```properties
openrouter.api.key=sk-tu-clave-aqui
```

### 2. Ejecutar el Servidor
Abre una terminal en la carpeta del proyecto y ejecuta:

```bash
mvn spring-boot:run
```

Una vez inicie, abre tu navegador en:
👉 **http://localhost:8080**

## 🎨 ¿Cómo actualizar el Diseño (Frontend)?

Este proyecto usa un sistema de **Frontend Desacoplado** para desarrollo, pero **Integrado** para producción.

- **¿Dónde edito el diseño?**
  Los diseñadores trabajan en la carpeta externa `../front/Front`. Ahí están los archivos fuente y Tailwind.

- **¿Cómo traigo los cambios al backend?**
  No copies archivos manualmente. Hemos creado un script que compila Tailwind y mueve todo a su lugar correcto automáticamente.
  
  Ejecuta en tu terminal (dentro de `LMMfunction`):
  ```bash
  ./integrate_front.sh
  ```
  
  *Esto actualizará la carpeta `src/main/resources/static` con la última versión del diseño.*

## 📦 Generar Ejecutable (Para Servidor/Producción)

Si necesitas generar un solo archivo `.jar` para subirlo a un VPS o compartirlo:

```bash
mvn clean package
```

El archivo se generará en la carpeta `target/` (ej. `IA-0.0.1-SNAPSHOT.jar`). Puedes ejecutarlo con:
```bash
java -jar target/IA-0.0.1-SNAPSHOT.jar
```

## 🛠️ Herramientas de Desarrollo

- **Consola H2 (Base de Datos)**: http://localhost:8080/h2-console
  - *User*: `sa`
  - *Password*: `password`
- **Test API Chat**: `http://localhost:8080/test/chat?message=Hola`
- **Limpieza de RAM**: El sistema limpia automáticamente las sesiones inactivas cada hora.

## ☁️ Colaboración (GitHub)

Para facilitar el trabajo en equipo y evitar conflictos, usa este script para guardar tu progreso:

```bash
./git_sync.sh
```

Este script hace todo por ti:
1.  **Te configura** automáticamente la primera vez (Nombre, Email y guarda tu clave).
2.  **Baja** los cambios de tus compañeros (`git pull`).
3.  **Sube** tus cambios a la nube (`git push`).