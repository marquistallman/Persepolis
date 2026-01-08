package com.persepolis.IA.Scraper.sitios;

import com.persepolis.IA.Scraper.model.WallpaperDTO;
import java.net.URLEncoder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WallpaperWaves extends SitioBase {
    @Override
    public String getNombre() { return "Wallpaper Waves"; }

    @Override
    public String generarUrlBusqueda(String query) throws Exception {
        return "https://wallpaperwaves.com/?s=" + URLEncoder.encode(query, "UTF-8");
    }

    @Override
    protected Elements obtenerElementosRelevantes(Document doc) {
        return buscarPorSelectores(doc, "article.jeg_post");
    }

    @Override
    public WallpaperDTO extraerDatos(Element elemento) {
        WallpaperDTO dto = new WallpaperDTO();
        Element titleElement = elemento.selectFirst("h3.jeg_post_title a");
        dto.setTitulo((titleElement != null) ? titleElement.text() : "Wallpaper Waves");
        dto.setEnlace((titleElement != null) ? titleElement.attr("href") : "");
        Element img = elemento.selectFirst(".jeg_thumb img");
        dto.setPreview((img != null) ? img.attr("src") : "");
        dto.setTipo("WallpaperWaves");
        dto.setHasVideo(true);
        return dto;
    }

    @Override
    public WallpaperDTO obtenerDetalles(String url) {
        WallpaperDTO detalles = new WallpaperDTO();
        try {
            Document doc = crearConexion(url).get();

            Element videoSource = doc.selectFirst("div.player_responsive video source");
            if (videoSource != null) {
                detalles.setVideoUrl(videoSource.attr("src"));
            }
        } catch (Exception e) {
            System.err.println("Error obteniendo detalles de WallpaperWaves: " + e.getMessage());
        }
        return detalles;
    }
}