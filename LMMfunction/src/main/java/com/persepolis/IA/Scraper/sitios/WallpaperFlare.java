package com.persepolis.IA.Scraper.sitios;

import com.persepolis.IA.Scraper.model.WallpaperDTO;
import java.net.URLEncoder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WallpaperFlare extends SitioBase {
    @Override
    public String getNombre() { return "Wallpaper Flare"; }

    @Override
    public String generarUrlBusqueda(String query, int page) throws Exception {
        return "https://www.wallpaperflare.com/search?wallpaper=" + URLEncoder.encode(query, "UTF-8") + "&page=" + page;
    }

    @Override
    public String getUrlPopulares(int page) {
        return "https://www.wallpaperflare.com/index.php?page=" + page;
    }

    @Override
    protected Elements obtenerElementosRelevantes(Document doc) {
        return buscarPorSelectores(doc, "a[itemprop='url']", "figure a");
    }

    @Override
    public WallpaperDTO extraerDatos(Element elemento) {
        WallpaperDTO dto = new WallpaperDTO();
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
        dto.setTitulo(titulo);
        dto.setEnlace((link != null) ? link.attr("href") : "");
        dto.setPreview(preview);
        dto.setResolucion(resolucion);
        dto.setTipo("WallpaperFlare");
        return dto;
    }
}