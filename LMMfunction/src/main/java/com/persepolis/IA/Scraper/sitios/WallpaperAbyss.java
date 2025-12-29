package com.persepolis.IA.Scraper.sitios;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
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
    public Elements obtenerElementosRelevantes(Document doc) {
        return buscarPorSelectores(doc, "div.boxgrid a", ".center-container a");
    }

    @Override
    public Map<String, String> extraerDatos(Element elemento) {
        Map<String, String> datos = new HashMap<>();
        String enlace = elemento.attr("href");
        if (!enlace.startsWith("http")) enlace = "https://wall.alphacoders.com/" + enlace;
        String titulo = elemento.attr("title");
        if (titulo.isEmpty() && elemento.selectFirst("img") != null) titulo = elemento.selectFirst("img").attr("alt");
        datos.put("titulo", titulo.isEmpty() ? "Wallpaper Abyss" : titulo);
        datos.put("enlace", enlace);
        datos.put("tipo", "Wallpaper Abyss");
        return datos;
    }
}