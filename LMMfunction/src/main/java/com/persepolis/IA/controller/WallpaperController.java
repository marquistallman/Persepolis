package com.persepolis.IA.controller;

import com.persepolis.IA.model.WallpaperItem;
import com.persepolis.IA.services.WallpaperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallpapers")
@CrossOrigin(origins = "*")
public class WallpaperController {

    @Autowired
    private WallpaperService service;

    @GetMapping("/search")
    public List<WallpaperItem> search(@RequestParam String query, @RequestParam(defaultValue = "1") int page) {
        return service.searchAndMerge(query, page);
    }

    @PostMapping("/interact")
    public void interact(@RequestBody Map<String, String> payload) {
        String url = payload.get("url");
        String html = payload.get("htmlContent");
        String type = payload.get("type");
        
        System.out.println("--- RECIBIDO INTERACCIÓN ---");
        System.out.println("Tipo: " + type);
        System.out.println("URL: " + url);

        service.processInteraction(url, html, type);
    }
}