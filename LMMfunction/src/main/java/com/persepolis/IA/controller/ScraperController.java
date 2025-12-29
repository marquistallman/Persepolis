package com.persepolis.IA.controller;

import com.persepolis.IA.Scraper.CScrap;
import com.persepolis.IA.Scraper.SiteDetailScraper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*") // Permite peticiones desde cualquier origen (necesario para conectar con el Front)
public class ScraperController {

    @GetMapping(value = "/scraper", produces = "application/json")
    public List<Map<String, String>> buscar(@RequestParam(name = "q", defaultValue = "anime") String query) {
        // Instanciamos el scraper
        CScrap scraper = new CScrap();
        
        // Llamamos al nuevo método que devuelve datos puros
        return scraper.buscarWeb(query);
    }

    @GetMapping(value = "/scraper/details", produces = "application/json")
    public Map<String, String> obtenerDetalles(@RequestParam String url, @RequestParam String site) {
        SiteDetailScraper detailScraper = new SiteDetailScraper();
        return detailScraper.obtenerDetalles(url, site);
    }
}
