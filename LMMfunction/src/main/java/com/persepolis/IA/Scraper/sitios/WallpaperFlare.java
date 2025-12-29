package com.persepolis.IA.Scraper.sitios;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WallpaperFlare extends SitioBase {
    @Override
    public String getNombre() { return "Wallpaper Flare"; }

    @Override
    public String generarUrlBusqueda(String query) throws Exception {
        return "https://www.wallpaperflare.com/search?wallpaper=" + URLEncoder.encode(query, "UTF-8");
    }

    @Override
    public Elements obtenerElementosRelevantes(Document doc) {
        return buscarPorSelectores(doc, "a[itemprop='url']", "figure a");
    }

    @Override
    public Map<String, String> extraerDatos(Element elemento) {
        Map<String, String> datos = new HashMap<>();
        Element figcaption = elemento.selectFirst("figcaption");
        String titulo = (figcaption != null) ? figcaption.text() : "WallpaperFlare";
        Element link = elemento.selectFirst("a[itemprop='url']");
        Element img = elemento.selectFirst("img");
        String preview = (img != null) ? img.attr("src") : "";
        if (preview.isEmpty() && img != null) preview = img.attr("data-src");

        String resolucion = "";
        if (img != null) {
            String texto = img.attr("alt") + " " + img.attr("title");
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d{3,4}x\\d{3,4}").matcher(texto);
            if (matcher.find()) resolucion = matcher.group();
        }
        datos.put("titulo", titulo);
        datos.put("enlace", (link != null) ? link.attr("href") : "");
        datos.put("preview", preview);
        datos.put("resolucion", resolucion);
        datos.put("tipo", "WallpaperFlare");
        return datos;
    }
}