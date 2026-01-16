# Documentación Técnica - Persepolis IA

Este documento describe la arquitectura técnica, los flujos de datos y los componentes principales del backend "Persepolis IA" para el sistema WallPaperSystem.

## 1. Visión General
El sistema es un backend desarrollado en **Java 21** con **Spring Boot** que actúa como cerebro para una aplicación de búsqueda de fondos de pantalla. Combina:
- **IA Simbólica**: Un árbol de decisión determinista para guiar al usuario.
- **IA Generativa (LLM)**: Fallback a modelos de lenguaje (vía OpenRouter) cuando el usuario no está satisfecho.
- **Web Scraping**: Extracción de contenido en tiempo real.
- **Caché Inteligente**: Sistema de caché en base de datos para minimizar peticiones externas.

## 2. Arquitectura del Sistema

### Controladores (Capa API)
- **`TestController`**: Endpoints de depuración y chat directo para pruebas. Gestiona sesiones en memoria RAM.
- **`ScraperController`**: API REST para realizar búsquedas de imágenes y obtener detalles.
- **`WallpaperController`**: Gestión de interacciones y búsquedas fusionadas.
- **`SpaController`** y **`HomeController`**: Manejo de rutas para servir el Frontend (Single Page Application).

### Servicios (Lógica de Negocio)
- **`ChatService`**: El núcleo de la interacción. Gestiona el estado de la conversación, navega el árbol de decisión (`chat_data.json`) y decide cuándo invocar al LLM.
- **`ScraperService`**: Intermediario entre la API y el scraper (`CScrap`). Implementa un patrón de caché *Read-Through* usando `WebCacheRepository`.
- **`OpenRouterService`** (Referenciado): Cliente para conectar con modelos LLM externos (Mistral, GPT, etc.).

### Datos y Persistencia
- **`chat_data.json`**: Define la estructura del árbol de decisión, palabras clave, *slang* y respuestas predefinidas. Permite cambiar la lógica del chat sin recompilar.
- **Base de Datos (H2/MySQL)**: Almacena el caché de búsquedas (`WebCache`) para evitar re-scrapear sitios frecuentemente.

## 3. Flujos de Datos Principales

### A. Flujo de Chat (ChatService)
1.  **Entrada**: El usuario envía un mensaje.
2.  **Clasificación**: Se detecta si es un saludo, una petición estándar o una duda.
3.  **Árbol de Decisión**: Si es una búsqueda guiada, el sistema navega por los nodos definidos en `chat_data.json` recolectando *keywords*.
4.  **Búsqueda**: Al finalizar el árbol, se ejecuta una búsqueda con las palabras clave acumuladas.
5.  **Evaluación**: Se pregunta al usuario si está satisfecho.
6.  **Fallback LLM**: Si el usuario dice "No", se envía el historial al LLM para generar nuevas combinaciones de búsqueda (Queries) y se reintenta.

### B. Flujo de Scraping con Caché (ScraperService)
1.  **Petición**: Llega una query (ej: "anime dark").
2.  **Check Caché**: Se consulta la BD por la clave `search:anime dark:1`.
    - Si existe y tiene < 12 horas: Se devuelve el JSON guardado.
3.  **Scraping (Miss)**: Si no existe o expiró, se instancia `CScrap` para buscar en la web.
4.  **Guardado**: Los resultados se serializan a JSON y se guardan en BD con timestamp actual.
5.  **Respuesta**: Se devuelve la lista de `WallpaperDTO`.

## 4. Detalle de Paquetes: Modelos y Scraper
A continuación se detalla la función de los archivos ubicados en `src/main/java`. Estos componentes son parte del código de producción.

### A. Paquete `model` (Persistencia)
- **`WebCache.java`**: Entidad JPA mapeada a la base de datos.
  - *Función*: Almacena el JSON crudo de una búsqueda para no repetir peticiones HTTP costosas.
  - *Campos Clave*: `cacheKey` (Query + Página), `content` (Resultados), `lastUpdated` (Para expiración).

### B. Paquete `Scraper.model` (Transferencia)
- **`WallpaperDTO.java`**: Objeto simple (POJO) que representa una imagen.
  - *Función*: Estandarizar la salida. Unifica los datos (título, enlace, thumbnail, dimensiones) independientemente de si la imagen viene de Wallhaven, Pinterest u otro sitio.

### C. Paquete `Scraper` (Lógica Core)
- **`CScrap.java`**: Clase orquestadora del motor de búsqueda.
  - *Función*: Recibe la petición del servicio, instancia los scrapers específicos, coordina la ejecución (secuencial o paralela) y fusiona las listas de resultados en una sola respuesta limpia.

### D. Paquete `Scraper.sitios` (Estrategias de Extracción)
Contiene la implementación individual para cada fuente de imágenes.
- **Archivos de Sitios** (ej. `Wallhaven.java`, `Pinterest.java`, etc.):
  - *Función*: Contienen la lógica específica para conectarse a la URL del proveedor, descargar el HTML y usar selectores (CSS/XPath) para extraer los enlaces de las imágenes.

### E. Paquete `dto` (Transferencia de Datos API)
- **`Message.java`**: Objeto para intercambio de mensajes.
  - *Función*: Estructura simple que define el formato de los mensajes de chat (rol y contenido) utilizados por el `TestController` y el frontend para mantener el historial de conversación.

## 5. Gestión de Memoria y Mantenimiento
El sistema incluye tareas programadas (`@Scheduled`) para mantener la salud del servidor:
- **Limpieza de Sesiones de Chat**: Cada hora se eliminan sesiones de chat inactivas de la RAM.
- **Limpieza de Caché Global**: Cada 30 minutos se limpia el caché nativo de Spring.
- **Evicción de Base de Datos**: Cada 24 horas se borran registros de caché de scraping antiguos (> 7 días).

## 6. Scripts de Automatización
- **`menu.ps1` / `menu.sh`**: Panel de control interactivo para operaciones comunes (Run, Git Sync, Build).
- **`git_sync.sh`**: Automatiza el flujo de Git (Pull -> Add -> Commit -> Push) gestionando credenciales.
- **`integrate_front.sh`** (en scripts): Compila el frontend (Tailwind) y mueve los estáticos a `src/main/resources/static`.