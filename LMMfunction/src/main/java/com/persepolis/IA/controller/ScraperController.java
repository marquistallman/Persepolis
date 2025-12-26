package com.persepolis.IA.controller;

import com.persepolis.IA.Scraper.CScrap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
public class ScraperController {

    @GetMapping("/scraper")
    public List<Map<String, String>> buscar(@RequestParam(name = "q", defaultValue = "anime") String query) {
        // Instanciamos el scraper
        CScrap scraper = new CScrap();
        
        // Llamamos al nuevo método que devuelve datos puros
        return scraper.buscarWeb(query);
    }
}

