package com.persepolis.IA.Scraper.sitios;

import com.persepolis.IA.Scraper.model.WallpaperDTO;
import java.net.URLEncoder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Wallhaven extends SitioBase {
    @Override
    public String getNombre() { return "Wallhaven"; }

    @Override
    public String generarUrlBusqueda(String query, int page) throws Exception {
        return "https://wallhaven.cc/search?q=" + URLEncoder.encode(query, "UTF-8") + "&page=" + page;
    }

    @Override
    public String getUrlPopulares(int page) {
        return "https://wallhaven.cc/toplist?purity=100&sorting=toplist&order=desc&page=" + page;
    }

    @Override
    protected Elements obtenerElementosRelevantes(Document doc) {
        return buscarPorSelectores(doc, "figure.thumb");
    }

    @Override
    public WallpaperDTO extraerDatos(Element elemento) {
        WallpaperDTO dto = new WallpaperDTO();
        Element linkElement = elemento.selectFirst("a.preview");
        String enlace = (linkElement != null) ? linkElement.attr("href") : "";
        if (!enlace.startsWith("http") && !enlace.isEmpty()) enlace = "https://wallhaven.cc" + (enlace.startsWith("/") ? enlace : "/" + enlace);

        String wallpaperId = elemento.attr("data-wallpaper-id");
        Element img = elemento.selectFirst("img");
        String preview = (img != null) ? img.attr("data-src") : "";
        if (preview.isEmpty() && img != null) preview = img.attr("src");
        
        Element resInfo = elemento.selectFirst(".wall-res");
        
        dto.setTitulo("Wallhaven " + wallpaperId);
        dto.setEnlace(enlace);
        dto.setPreview(preview);
        dto.setResolucion((resInfo != null) ? resInfo.text() : "");
        dto.setTipo("Wallhaven");
        return dto;
    }
    @Override
    public WallpaperDTO obtenerDetalles(String url) {
        WallpaperDTO detalles = new WallpaperDTO();
        try {
            Document doc = crearConexion(url).get();
            
            Element img = doc.selectFirst("img#wallpaper");
            if (img != null) {
                detalles.setFullImageUrl(img.attr("src"));
            }
            
            // Extracción de Tags
            Elements tags = doc.select("ul#tags li a.tagname");
            for (Element tag : tags) {
                detalles.addTag(tag.text());
            }
        } catch (Exception e) {
            System.err.println("Error obteniendo detalles de Wallhaven: " + e.getMessage());
        }
        return detalles;
    }
}