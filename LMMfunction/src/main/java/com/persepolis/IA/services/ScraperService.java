package com.persepolis.IA.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.persepolis.IA.Scraper.CScrap;
import com.persepolis.IA.Scraper.model.WallpaperDTO;
import com.persepolis.IA.model.WebCache;
import com.persepolis.IA.repository.WebCacheRepository;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Servicio encargado de la obtención de wallpapers.
 * Actúa como una capa de abstracción sobre el Scraper (CScrap), añadiendo
 * persistencia y caché (WebCacheRepository) para optimizar tiempos de respuesta.
 */
@Service("iaScraperService")
public class ScraperService {

    private final WebCacheRepository webCacheRepository;
    private final ObjectMapper objectMapper;
    private final ColorPaletteService colorPaletteService;
    private static final long SEARCH_CACHE_HOURS = 12; // Tiempo de vida para el caché de búsquedas
    private static final long DETAILS_CACHE_DAYS = 7;  // Tiempo de vida para el caché de detalles

    public ScraperService(WebCacheRepository webCacheRepository, ColorPaletteService colorPaletteService) {
        this.webCacheRepository = webCacheRepository;
        this.colorPaletteService = colorPaletteService;
        this.objectMapper = new ObjectMapper();
    }

    public List<WallpaperDTO> searchWallpapers(String query) {
        return searchWallpapers(query, 1);
    }

    /**
     * Busca wallpapers por término y página.
     * Implementa patrón "Read-Through Cache":
     * 1. Busca en BD si existe una búsqueda reciente (< 12 horas).
     * 2. Si no, ejecuta el scraper real y guarda el resultado.
     *
     * @param query Término de búsqueda.
     * @param page Número de página.
     * @return Lista de wallpapers encontrados.
     */
    public List<WallpaperDTO> searchWallpapers(String query, int page) {
        String normalizedQuery = query.trim().toLowerCase();
        String cacheKey = "search:" + normalizedQuery + ":" + page;

        Optional<WebCache> cached = webCacheRepository.findByCacheKey(cacheKey);

        if (cached.isPresent()) {
            WebCache data = cached.get();
            long hours = ChronoUnit.HOURS.between(data.getLastUpdated(), LocalDateTime.now());
            if (hours < SEARCH_CACHE_HOURS) {
                System.out.println("--- Devolviendo resultados de búsqueda desde CACHÉ para: " + query);
                return prioritizeTitleMatches(fromJsonToList(data.getContent()), normalizedQuery);
            }
        }

        try {
            System.out.println("--- Realizando búsqueda REAL para: " + query);
            CScrap scraper = new CScrap();
            List<WallpaperDTO> results = scraper.buscarWeb(normalizedQuery, page);
            
            // DEBUG: log cantidad antes de guardar
            System.out.println("--- DEBUG: Scraper encontró (antes guardar) " + results.size() + " items ---");

            saveToCache(cacheKey, toJson(results));
            return prioritizeTitleMatches(results, normalizedQuery);
        } catch (Exception e) {
            System.err.println("--- ERROR en ScraperService (Búsqueda): " + e.getMessage());
            e.printStackTrace();
            return List.of(); // Retorna lista vacía para no romper el front
        }
    }

    private List<WallpaperDTO> prioritizeTitleMatches(List<WallpaperDTO> results, String query) {
        if (results == null || results.isEmpty()) {
            return results;
        }
        return results.stream()
            .sorted((item1, item2) -> {
                boolean match1 = titleContainsRequest(item1, query);
                boolean match2 = titleContainsRequest(item2, query);
                if (match1 && !match2) return -1;
                if (!match1 && match2) return 1;
                return 0;
            })
            .collect(java.util.stream.Collectors.toList());
    }

    private boolean titleContainsRequest(WallpaperDTO item, String request) {
        if (item == null || item.getTitulo() == null || request == null) return false;
        return item.getTitulo().toLowerCase(java.util.Locale.ROOT).contains(request);
    }

    // Métodos de ayuda para depuración
    public long countCacheEntries() {
        try { return webCacheRepository.count(); } catch (Exception e) { return -1; }
    }

    public java.util.List<java.util.Map<String, Object>> getCacheSample(int limit) {
        try {
            java.util.List<WebCache> list = webCacheRepository.findAll();
            java.util.List<java.util.Map<String, Object>> out = new java.util.ArrayList<>();
            for (int i = 0; i < Math.min(limit, list.size()); i++) {
                WebCache w = list.get(i);
                java.util.Map<String, Object> m = new java.util.HashMap<>();
                m.put("id", w.getId());
                m.put("cacheKey", w.getCacheKey());
                m.put("urlOrQuery", w.getUrlOrQuery());
                m.put("lastUpdated", w.getLastUpdated());
                out.add(m);
            }
            return out;
        } catch (Exception e) {
            return java.util.List.of();
        }
    }

    /**
     * Obtiene los detalles de una imagen específica.
     * Utiliza un tiempo de caché más largo (7 días) ya que los detalles de una imagen
     * raramente cambian.
     */
    public WallpaperDTO getWallpaperDetails(String url, String site) {
        String cacheKey = "details:" + site + ":" + url;

        Optional<WebCache> cached = webCacheRepository.findByCacheKey(cacheKey);

        if (cached.isPresent()) {
            WebCache data = cached.get();
            long days = ChronoUnit.DAYS.between(data.getLastUpdated(), LocalDateTime.now());
            if (days < DETAILS_CACHE_DAYS) {
                 System.out.println("--- Devolviendo detalles desde CACHÉ para: " + url);
                return fromJsonToDto(data.getContent());
            }
        }

        try {
            System.out.println("--- Obteniendo detalles REALES para: " + url);
            CScrap scraper = new CScrap();
            WallpaperDTO details = scraper.obtenerDetalles(url, site);
            
            // Extraer paleta de colores de la preview
            if (details.getPreview() != null && !details.getPreview().isEmpty()) {
                details.setPalette(colorPaletteService.extractPalette(details.getPreview(), 5));
            }

            saveToCache(cacheKey, toJson(details));
            return details;
        } catch (Exception e) {
            System.err.println("--- ERROR en ScraperService (Detalles): " + e.getMessage());
            e.printStackTrace();
            return new WallpaperDTO();
        }
    }

    // --- GESTIÓN DE MEMORIA Y DISCO ---
    /**
     * Tarea programada: Mantenimiento de la base de datos.
     * Elimina registros de caché que tienen más de 8 días de antigüedad para liberar espacio.
     */
    @Scheduled(fixedRate = 86400000)
    public void evictOldCache() {
        try {
            System.out.println("--- MANTENIMIENTO: Iniciando limpieza de caché antiguo...");
            List<WebCache> allItems = webCacheRepository.findAll();
            List<WebCache> toDelete = allItems.stream()
                .filter(item -> ChronoUnit.DAYS.between(item.getLastUpdated(), LocalDateTime.now()) > DETAILS_CACHE_DAYS + 1)
                .toList();
            
            if (!toDelete.isEmpty()) {
                webCacheRepository.deleteAll(toDelete);
                System.out.println("--- MANTENIMIENTO: Eliminados " + toDelete.size() + " registros antiguos.");
            }
        } catch (Exception e) {
            System.err.println("--- ERROR en limpieza de caché: " + e.getMessage());
        }
    }

    private void saveToCache(String key, String jsonContent) {
        try {
            WebCache item = webCacheRepository.findByCacheKey(key).orElse(new WebCache());
            item.setCacheKey(key);
            item.setUrlOrQuery(key);
            item.setContent(jsonContent);
            item.setLastUpdated(LocalDateTime.now());
            webCacheRepository.save(item);
            System.out.println("--- DEBUG: Cache guardado para key='" + key + "' ---");
        } catch (Exception e) {
            System.err.println("--- ERROR guardando cache para key='" + key + "': " + e.getMessage());
            e.printStackTrace();
            // No re-throw: no queremos que fallos de cache impidan devolver resultados
        }
    }

    private String toJson(Object object) {
        try { return objectMapper.writeValueAsString(object); } catch (Exception e) { return "{}"; }
    }

    private List<WallpaperDTO> fromJsonToList(String json) {
        try { return objectMapper.readValue(json, new TypeReference<List<WallpaperDTO>>() {}); } catch (Exception e) { return List.of(); }
    }

    private WallpaperDTO fromJsonToDto(String json) {
        try { return objectMapper.readValue(json, WallpaperDTO.class); } catch (Exception e) { return new WallpaperDTO(); }
    }
}