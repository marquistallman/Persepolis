package com.persepolis.IA.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SitemapController {

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Página Principal
        xml.append("  <url>\n");
        xml.append("    <loc>https://persepoliasia.lat/pages/homepage.html</loc>\n");
        xml.append("    <changefreq>daily</changefreq>\n");
        xml.append("    <priority>1.0</priority>\n");
        xml.append("  </url>\n");

        // Categorías Principales (Hardcoded por ahora, idealmente dinámico)
        String[] categories = {"anime", "nature", "abstract", "minimalist", "cyberpunk", "dark"};
        
        for (String cat : categories) {
            xml.append("  <url>\n");
            xml.append("    <loc>https://persepoliasia.lat/pages/browse.html?q=").append(cat).append("</loc>\n");
            xml.append("    <changefreq>weekly</changefreq>\n");
            xml.append("    <priority>0.8</priority>\n");
            xml.append("  </url>\n");
        }

        xml.append("</urlset>");
        return xml.toString();
    }
}