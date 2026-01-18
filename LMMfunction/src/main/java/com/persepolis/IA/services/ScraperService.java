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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        String cacheKey = "details:" + site + ":" + url + ":v2";

        Optional<WebCache> cached = webCacheRepository.findByCacheKey(cacheKey);

        if (cached.isPresent()) {
            WebCache data = cached.get();
            long days = ChronoUnit.DAYS.between(data.getLastUpdated(), LocalDateTime.now());
            if (days < DETAILS_CACHE_DAYS) {
                 System.out.println("--- Devolviendo detalles desde CACHÉ para: " + url);
                WallpaperDTO dto = fromJsonToDto(data.getContent());
                
                // Si la paleta no existe en caché (versiones viejas), intentamos extraerla ahora
                if (dto.getPalette() == null || dto.getPalette().size() < 5) {
                    String img = dto.getPreview();
                    
                    // Si no hay preview, intentar extraerla del HTML original
                    if (img == null || img.isEmpty()) {
                        img = extractPreviewFromHtml(dto.getEnlace());
                        if (img != null && !img.isEmpty()) dto.setPreview(img);
                    }
                    
                    if (img == null || img.isEmpty()) img = dto.getFullImageUrl();
                    
                    if (img != null && !img.isEmpty()) {
                        System.out.println("--- CACHE: Paleta insuficiente/faltante, re-extrayendo de: " + img);
                        dto.setPalette(colorPaletteService.extractPalette(img, 5));
                        if (dto.getPalette() != null && !dto.getPalette().isEmpty()) saveToCache(cacheKey, toJson(dto));
                    }
                }
                return dto;
            }
        }

        try {
            System.out.println("--- Obteniendo detalles REALES para: " + url);
            CScrap scraper = new CScrap();
            WallpaperDTO details = scraper.obtenerDetalles(url, site);
            
            System.out.println("--- DEBUG: fullImageUrl after obtenerDetalles: " + details.getFullImageUrl());
            System.out.println("--- DEBUG: preview after obtenerDetalles: " + details.getPreview());
            System.out.println("--- DEBUG: palette size after obtenerDetalles: " + (details.getPalette() != null ? details.getPalette().size() : "null"));
            
            // Extraer paleta de colores usando la imagen del preview
            String imgToProcess = details.getPreview();
            
            // Si no hay preview, intentar extraerla del HTML
            if (imgToProcess == null || imgToProcess.isEmpty()) {
                imgToProcess = extractPreviewFromHtml(url);
                if (imgToProcess != null && !imgToProcess.isEmpty()) {
                    System.out.println("--- DEBUG: Preview recuperada del HTML: " + imgToProcess);
                    details.setPreview(imgToProcess);
                }
            }
            
            System.out.println("--- DEBUG: imgToProcess final: " + imgToProcess);
            
            if ((details.getPalette() == null || details.getPalette().size() < 5) && imgToProcess != null && !imgToProcess.isEmpty()) {
                System.out.println("--- SCRAPER: Iniciando extracción de paleta para: " + imgToProcess);
                details.setPalette(colorPaletteService.extractPalette(imgToProcess, 5));
                System.out.println("--- DEBUG: Palette extraída, size: " + details.getPalette().size());
            } else {
                System.out.println("--- DEBUG: No se extrajo paleta, condición no cumplida");
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

    /**
     * Método auxiliar para extraer la URL de la imagen de previsualización desde el HTML.
     * Busca la etiqueta <img id="previewImage" src="...">
     */
    private String extractPreviewFromHtml(String pageUrl) {
        if (pageUrl == null || pageUrl.isEmpty()) return null;
        try {
            URL url = URI.create(pageUrl.replace(" ", "%20")).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            conn.setConnectTimeout(5000);
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) content.append(line);
                
                String html = content.toString();
                
                // 1. Intentar con Open Graph (Estándar global: og:image)
                Matcher mOg = Pattern.compile("<meta\\s+property=[\"']og:image[\"']\\s+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE).matcher(html);
                if (mOg.find()) return mOg.group(1);
                
                // 2. Intentar con Open Graph invertido (content antes de property)
                Matcher mOg2 = Pattern.compile("<meta\\s+content=[\"']([^\"']+)[\"']\\s+property=[\"']og:image[\"']", Pattern.CASE_INSENSITIVE).matcher(html);
                if (mOg2.find()) return mOg2.group(1);
                
                // 3. Intentar con ID específico (Legacy)
                Matcher mId = Pattern.compile("id=[\"']previewImage[\"'][^>]*src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE).matcher(html);
                if (mId.find()) return mId.group(1);
            }
        } catch (Exception e) {
            System.err.println("--- Error extrayendo preview del HTML (" + pageUrl + "): " + e.getMessage());
        }
        return null;
    }
}