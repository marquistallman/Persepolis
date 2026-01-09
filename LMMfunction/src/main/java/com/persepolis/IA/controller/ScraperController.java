package com.persepolis.IA.controller;

import com.persepolis.IA.Scraper.model.WallpaperDTO;
import com.persepolis.IA.services.ScraperService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@RestController
@CrossOrigin(origins = "*") // Permite peticiones desde cualquier origen (necesario para conectar con el Front)
public class ScraperController {

    private final ScraperService scraperService;

    public ScraperController(ScraperService scraperService) {
        this.scraperService = scraperService;
    }

    @GetMapping(value = "/scraper", produces = "application/json")
    public List<WallpaperDTO> buscar(@RequestParam(name = "q", defaultValue = "anime") String query, @RequestParam(name = "page", defaultValue = "1") int page) {
        return scraperService.searchWallpapers(query, page);
    }

    @GetMapping(value = "/scraper/details", produces = "application/json")
    public WallpaperDTO obtenerDetalles(@RequestParam String url, @RequestParam String site) {
        return scraperService.getWallpaperDetails(url, site);
    }

    // Endpoint de depuración: muestra conteos y primeras filas de tablas relevantes
    @org.springframework.web.bind.annotation.GetMapping(value = "/debug/db", produces = "application/json")
    public java.util.Map<String, Object> debugDb() {
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("web_cache_count", scraperService.countCacheEntries());
        resp.put("web_cache_samples", scraperService.getCacheSample(10));
        return resp;
    }
}
