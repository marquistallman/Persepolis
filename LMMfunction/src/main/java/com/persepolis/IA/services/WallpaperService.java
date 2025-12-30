package com.persepolis.IA.services;

import com.persepolis.IA.model.WallpaperItem;
import com.persepolis.IA.repository.WallpaperRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WallpaperService {

    @Autowired
    private WallpaperRepository repository;

    private final WebClient webClient;
    private final ObjectMapper mapper;

    @org.springframework.beans.factory.annotation.Autowired
    private ScraperService scraperService; // Fallback para ejecutar el scraper in-process si la llamada HTTP falla

    public WallpaperService() {
        this.webClient = WebClient.create("http://localhost:8080");
        this.mapper = new ObjectMapper();
    }

    // Método principal de búsqueda
    public List<WallpaperItem> searchAndMerge(String query) {
        // 1. Buscar en Base de Datos
        List<WallpaperItem> dbResults = repository.findByHtmlContentContainingIgnoreCase(query);

        // 2. Ordenar DESCENDENTE por puntos (Más populares primero)
        dbResults.sort((a, b) -> Integer.compare(b.getTotalScore(), a.getTotalScore()));

        // Si la BD está vacía, hacer scraping in-process, guardar resultados y devolverlos
        if (dbResults.isEmpty()) {
            try {
                List<java.util.Map<String, String>> scraped = scraperService.searchWallpapers(query);
                System.out.println("--- DEBUG: DB vacía, scraper in-process devolvió " + scraped.size() + " items ---");
                if (!scraped.isEmpty()) {
                    List<WallpaperItem> savedItems = new ArrayList<>();
                    for (java.util.Map<String, String> map : scraped) {
                        try {
                            String jsonContent = mapper.writeValueAsString(map);
                            String url = (String) map.getOrDefault("enlace", map.getOrDefault("url", String.valueOf(jsonContent.hashCode())));
                            WallpaperItem saved = saveScraperResult(jsonContent, url);
                            if (saved != null) savedItems.add(saved); else System.out.println("Nota: saveScraperResult devolvió null para url=" + url);
                        } catch (Exception ex) {
                            System.out.println("Nota: Error guardando item del scraper: " + ex.getMessage());
                            ex.printStackTrace();
                        }
                    }
                    // Ordenar y devolver lo guardado
                    savedItems.sort((a, b) -> Integer.compare(b.getTotalScore(), a.getTotalScore()));
                    System.out.println("--- DEBUG: Guardados " + savedItems.size() + " items en BD (desde scraper inicial) ---");
                    return savedItems;
                } else {
                    System.out.println("--- DEBUG: Scraper in-process no devolvió items para query: " + query + " ---");
                }
            } catch (Exception e) {
                System.out.println("Nota: Falla al ejecutar scraper in-process: " + e.getMessage());
            }
        }

        // 3. Obtener resultados del Scraper
        List<WallpaperItem> scraperResults = callYourScraper(query);

        // 4. Filtrar Scraper: Si ya está en DB, no lo incluimos de nuevo
        // Usamos un Set de URLs de la DB para búsqueda rápida
        Set<String> dbUrls = dbResults.stream()
                .map(WallpaperItem::getUrl)
                .collect(Collectors.toSet());

        List<WallpaperItem> newScraperResults = scraperResults.stream()
                .filter(item -> !dbUrls.contains(item.getUrl()))
                .toList();

        // 5. Combinar: Primero DB, luego Scraper nuevos
        List<WallpaperItem> finalResults = new ArrayList<>(dbResults);
        finalResults.addAll(newScraperResults);

        return finalResults;
    }

    private List<WallpaperItem> callYourScraper(String query) {
        try {
            String jsonResponse = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/scraper").queryParam("q", query).build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (jsonResponse == null || jsonResponse.isEmpty()) {
                System.out.println("--- DEBUG: El scraper devolvió respuesta vacía ---");
                // Fallback: intentar ejecutar el scraper localmente (in-process)
                try {
                    List<java.util.Map<String, String>> fallback = scraperService.searchWallpapers(query);
                    System.out.println("--- DEBUG: Fallback in-process devolvió " + fallback.size() + " items ---");
                    return fallback.stream().map(map -> {
                        try {
                            String jsonContent = mapper.writeValueAsString(map);
                            String url = (String) map.getOrDefault("enlace", map.getOrDefault("url", String.valueOf(jsonContent.hashCode())));
                            return new WallpaperItem(jsonContent, url);
                        } catch (Exception ex) {
                            return null;
                        }
                    }).filter(item -> item != null).collect(Collectors.toList());
                } catch (Exception ex) {
                    System.out.println("Nota: Fallback in-process falló: " + ex.getMessage());
                    return new ArrayList<>();
                }
            }

            List<Map<String, Object>> rawItems = mapper.readValue(jsonResponse, new TypeReference<List<Map<String, Object>>>(){});
            System.out.println("--- DEBUG: Scraper encontró " + rawItems.size() + " items ---");
            
            // Si la respuesta HTTP está vacía de items, también intentamos el fallback in-process
            if (rawItems.isEmpty()) {
                try {
                    List<java.util.Map<String, String>> fallback = scraperService.searchWallpapers(query);
                    System.out.println("--- DEBUG: Fallback in-process devolvió " + fallback.size() + " items ---");
                    return fallback.stream().map(map -> {
                        try {
                            String jsonContent = mapper.writeValueAsString(map);
                            String url = (String) map.getOrDefault("enlace", map.getOrDefault("url", String.valueOf(jsonContent.hashCode())));
                            return new WallpaperItem(jsonContent, url);
                        } catch (Exception ex) {
                            return null;
                        }
                    }).filter(item -> item != null).collect(Collectors.toList());
                } catch (Exception ex) {
                    System.out.println("Nota: Fallback in-process falló: " + ex.getMessage());
                    return new ArrayList<>();
                }
            }

            return rawItems.stream().map(map -> {
                try {
                    String jsonContent = mapper.writeValueAsString(map);
                    String url = (String) map.getOrDefault("enlace", map.getOrDefault("url", String.valueOf(jsonContent.hashCode())));
                    return new WallpaperItem(jsonContent, url);
                } catch (Exception e) {
                    return null;
                }
            }).filter(item -> item != null).collect(Collectors.toList());

        } catch (Exception e) {
            System.out.println("Nota: No se pudo obtener datos del scraper (/scraper): " + e.getMessage());
            // Intentar fallback in-process en caso de excepción
            try {
                List<java.util.Map<String, String>> fallback = scraperService.searchWallpapers(query);
                System.out.println("--- DEBUG: Fallback in-process devolvió " + fallback.size() + " items (después de excepción) ---");
                return fallback.stream().map(map -> {
                    try {
                        String jsonContent = mapper.writeValueAsString(map);
                        String url = (String) map.getOrDefault("enlace", map.getOrDefault("url", String.valueOf(jsonContent.hashCode())));
                        return new WallpaperItem(jsonContent, url);
                    } catch (Exception ex) {
                        return null;
                    }
                }).filter(item -> item != null).collect(Collectors.toList());
            } catch (Exception ex) {
                System.out.println("Nota: Fallback in-process también falló: " + ex.getMessage());
                return new ArrayList<>();
            }
        }
    }

    // Métodos para registrar acciones (llamar desde el Controller cuando el usuario haga click)
    public void registerDownload(Long id) {
        repository.findById(id).ifPresent(item -> {
            item.incrementDownloads();
            repository.save(item);
        });
    }

    public void registerRedirect(Long id) {
        repository.findById(id).ifPresent(item -> {
            item.incrementRedirects();
            repository.save(item);
        });
    }

    // Método para guardar resultados del scraper en la BD si el usuario interactúa con ellos
    public WallpaperItem saveScraperResult(String html, String url) {
        return repository.findByUrl(url)
                .orElseGet(() -> repository.save(new WallpaperItem(html, url)));
    }

    // Método unificado para procesar interacciones desde el Frontend
    public WallpaperItem processInteraction(String url, String html, String type) {
        try {
            WallpaperItem item = repository.findByUrl(url)
                    .orElse(new WallpaperItem(html, url)); // Si no existe (viene del scraper), se crea en memoria

            if ("download".equalsIgnoreCase(type)) {
                item.incrementDownloads();
            } else if ("redirect".equalsIgnoreCase(type)) {
                item.incrementRedirects();
            }

            WallpaperItem saved = repository.save(item); // Guarda o actualiza en la BD
            System.out.println("--- GUARDADO EN BD: ID=" + saved.getId() + " Puntos=" + saved.getTotalScore() + " ---");
            return saved;
        } catch (Exception e) {
            System.err.println("--- ERROR AL GUARDAR INTERACCIÓN: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}