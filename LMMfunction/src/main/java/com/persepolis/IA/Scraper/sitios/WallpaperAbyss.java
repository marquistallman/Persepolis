package com.persepolis.IA.Scraper.sitios;

import com.persepolis.IA.Scraper.model.WallpaperDTO;
import java.net.URLEncoder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WallpaperAbyss extends SitioBase {
    @Override
    public String getNombre() { return "Wallpaper Abyss"; }

    @Override
    public String generarUrlBusqueda(String query) throws Exception {
        return "https://wall.alphacoders.com/search.php?search=" + URLEncoder.encode(query, "UTF-8");
    }

    @Override
    protected Elements obtenerElementosRelevantes(Document doc) {
        return buscarPorSelectores(doc, "div.boxgrid a", ".center-container a");
    }

    @Override
    public WallpaperDTO extraerDatos(Element elemento) {
        WallpaperDTO dto = new WallpaperDTO();
        String enlace = elemento.attr("href");
        if (!enlace.startsWith("http")) enlace = "https://wall.alphacoders.com/" + enlace;
        String titulo = elemento.attr("title");
        if (titulo.isEmpty() && elemento.selectFirst("img") != null) titulo = elemento.selectFirst("img").attr("alt");
        dto.setTitulo(titulo.isEmpty() ? "Wallpaper Abyss" : titulo);
        dto.setEnlace(enlace);
        dto.setTipo("Wallpaper Abyss");
        return dto;
    }
}