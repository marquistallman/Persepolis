package com.persepolis.IA.Scraper.sitios;

import com.persepolis.IA.Scraper.model.WallpaperDTO;
import java.net.URLEncoder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Peakpx extends SitioBase {
    @Override
    public String getNombre() { return "Peakpx"; }

    @Override
    public String generarUrlBusqueda(String query, int page) throws Exception {
        return "https://www.peakpx.com/en/search?q=" + URLEncoder.encode(query, "UTF-8") + "&page=" + page;
    }

    @Override
    public String getUrlPopulares(int page) {
        return "https://www.peakpx.com/en/page/" + page;
    }

    @Override
    protected Elements obtenerElementosRelevantes(Document doc) {
        return buscarPorSelectores(doc, "#search-list li");
    }

    @Override
    public WallpaperDTO extraerDatos(Element elemento) {
        WallpaperDTO dto = new WallpaperDTO();
        Element link = elemento.selectFirst("a[itemprop='url']");
        String enlace = (link != null) ? link.attr("href") : "";
        if (!enlace.startsWith("http") && !enlace.isEmpty()) enlace = "https://www.peakpx.com" + enlace;
        Element img = elemento.selectFirst("img");
        String preview = (img != null) ? (img.hasAttr("data-src") ? img.attr("data-src") : img.attr("src")) : "";
        Element caption = elemento.selectFirst("figcaption");
        Element resSpan = elemento.selectFirst("span.res");
        dto.setTitulo((caption != null) ? caption.text() : "Peakpx Wallpaper");
        dto.setEnlace(enlace);
        dto.setPreview(preview);
        dto.setResolucion((resSpan != null) ? resSpan.text() : "");
        dto.setTipo("Peakpx");
        return dto;
    }
}