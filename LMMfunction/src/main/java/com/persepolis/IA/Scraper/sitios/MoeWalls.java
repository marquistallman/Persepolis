package com.persepolis.IA.Scraper.sitios;

import com.persepolis.IA.Scraper.model.WallpaperDTO;
import java.net.URLEncoder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MoeWalls extends SitioBase {
    @Override
    public String getNombre() { return "Moe Walls"; }

    @Override
    public String generarUrlBusqueda(String query, int page) throws Exception {
        if (page > 1) return "https://moewalls.com/page/" + page + "/?s=" + URLEncoder.encode(query, "UTF-8");
        return "https://moewalls.com/?s=" + URLEncoder.encode(query, "UTF-8");
    }

    @Override
    public String getUrlPopulares(int page) {
        if (page > 1) return "https://moewalls.com/page/" + page + "/";
        return "https://moewalls.com/";
    }

    @Override
    protected Elements obtenerElementosRelevantes(Document doc) {
        return buscarPorSelectores(doc, "article.post");
    }

    @Override
    public WallpaperDTO extraerDatos(Element elemento) {
        WallpaperDTO dto = new WallpaperDTO();
        Element titleElement = elemento.selectFirst("h3.entry-title a");
        dto.setTitulo((titleElement != null) ? titleElement.text() : "Moe Walls Wallpaper");
        dto.setEnlace((titleElement != null) ? titleElement.attr("href") : "");
        
        Element img = elemento.selectFirst(".entry-featured-media img");
        dto.setPreview((img != null) ? img.attr("src") : "");
        
        Element resElement = elemento.selectFirst(".entry-resolutions a");
        dto.setResolucion((resElement != null) ? resElement.text() : "");
        
        // Extracción de Tags desde las clases CSS (ej: tag-anime, category-movies)
        for (String className : elemento.classNames()) {
            if (className.startsWith("tag-")) {
                dto.addTag(className.substring(4).replace("-", " "));
            } else if (className.startsWith("category-")) {
                dto.addTag(className.substring(9).replace("-", " "));
            }
        }
        
        dto.setTipo("Moe Walls");
        dto.setHasVideo(true);
        return dto;
    }

    @Override
    public WallpaperDTO obtenerDetalles(String url) {
        WallpaperDTO detalles = new WallpaperDTO();
        try {
            Document doc = crearConexion(url).get();

            // Buscamos el video y usamos abs:src para obtener la URL completa (incluyendo dominio)
            Element video = doc.selectFirst("video");
            if (video != null) {
                String src = video.attr("abs:src");
                if (src.isEmpty()) {
                    Element source = video.selectFirst("source");
                    if (source != null) src = source.attr("abs:src");
                }
                
                if (!src.isEmpty()) {
                    detalles.setVideoUrl(src);
                }
            }
        } catch (Exception e) {
            System.err.println("Error obteniendo detalles de Moewalls: " + e.getMessage());
        }
        return detalles;
    }
}