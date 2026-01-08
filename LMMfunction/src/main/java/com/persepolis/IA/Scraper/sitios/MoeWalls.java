package com.persepolis.IA.Scraper.sitios;

import com.persepolis.IA.Scraper.model.WallpaperDTO;
import java.net.URLEncoder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MoeWalls extends SitioBase {
    @Override
    public String getNombre() { return "Moe Walls"; }

    @Override
    public String generarUrlBusqueda(String query) throws Exception {
        return "https://moewalls.com/?s=" + URLEncoder.encode(query, "UTF-8");
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