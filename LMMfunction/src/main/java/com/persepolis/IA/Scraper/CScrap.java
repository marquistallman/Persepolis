package com.persepolis.IA.Scraper;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;



public class CScrap {
    
    private List<ConfiguracionSitio> sitiosConfigurados; 
    
    public CScrap(){
    
        inicializarSitios(); 
    
}
    
    private void inicializarSitios(){
        
        sitiosConfigurados = new ArrayList<>();
        
                sitiosConfigurados.add(new ConfiguracionSitio(
        
                "Wallpaper Waves",
                "https://wallpaperwaves.com/",
                "s",
                "/",
                new String[]{"article.jeg_post"} 
        ));
        
                sitiosConfigurados.add(new ConfiguracionSitio(
        
                "Motion Backgrounds",
                "https://motionbgs.com/",
                null,
                "/tag/",
                new String[]{"a[title$='live wallpaper download']",  
                             "figure a[href^='/']", 
                             ".grid-item a"  } 
        ));
                
                sitiosConfigurados.add(new ConfiguracionSitio(
        
                "Moe Walls",
                "https://moewalls.com/",
                "s",
                "/",
                new String[]{"h3.entry-title a", "article a[href*='/']"} 
        ));
                sitiosConfigurados.add(new ConfiguracionSitio(
        
                "Wallhaven",
                "https://wallhaven.cc",
                "q",
                "/search",
                new String[]{"figure.thumb"} 
        ));
               
                sitiosConfigurados.add(new ConfiguracionSitio(
        
                "Wallpaper Flare",
                "https://www.wallpaperflare.com/",
                "wallpaper",
                "/search",
                new String[]{"a[itemprop='url']", "figure a"} 
        ));
                sitiosConfigurados.add(new ConfiguracionSitio(
        
                "Artvee",
                "https://artvee.com/",
                "s",
                "/main/",
                new String[]{"h3.product-title a", "a[href*='/dl/']"} 
        ));
                
                sitiosConfigurados.add(new ConfiguracionSitio(
        
                "Wallpaper Abyss",
                "https://wall.alphacoders.com/",
                "search",
                "search.php",
                new String[]{"div.boxgrid a", ".center-container a"} 
        ));

                sitiosConfigurados.add(new ConfiguracionSitio(
                        "Peakpx",
                        "https://www.peakpx.com/",
                        "q",
                        "en/search",
                        new String[]{"figure a", "#search-list li a"} 
        ));
               
    }
    
     private String construirURLCorrecta(ConfiguracionSitio config, String busqueda) {
       try {
        if (config.nombre.equals("Wallpaper Waves")) {
            String busquedaCodificada = URLEncoder.encode(busqueda, "UTF-8");
            return config.urlBase + "?s=" + busquedaCodificada;
        }
        
        else if (config.nombre.equals("Motion Backgrounds")) {
            String busquedaFormateada = busqueda.toLowerCase().replace(" ", "-");
    // CORREGIDO: Usar "tag:" en lugar de "tag/"
    return config.urlBase + "tag:" + busquedaFormateada + "/";
        }
        
        else if (config.nombre.equals("Wallhaven")) {
            return config.urlBase + "/search?q=" + URLEncoder.encode(busqueda, "UTF-8");
        }
        
        else if (config.nombre.equals("Wallpaper Flare")) {
            return config.urlBase + "search?wallpaper=" + URLEncoder.encode(busqueda, "UTF-8");
        }
        
        else if (config.nombre.equals("Artvee")) {
            return config.urlBase + "main/?s=" + URLEncoder.encode(busqueda, "UTF-8");
        }
        
        else if (config.nombre.equals("Moe Walls")) {
            return config.urlBase + "?s=" + URLEncoder.encode(busqueda, "UTF-8");
        }
        
        else if (config.nombre.equals("Wallpaper Abyss")) {
            return config.urlBase + "search.php?search=" + URLEncoder.encode(busqueda, "UTF-8");
        }
        
        else if (config.nombre.equals("Peakpx")) {
            return config.urlBase + "en/search?q=" + URLEncoder.encode(busqueda, "UTF-8");
        }
        return config.urlBase;
        
    } catch (Exception e) {
        return config.urlBase;
    }
    }

    private Elements buscarElementos(Document doc, String[] selectores) {
        Elements resultados = new Elements();
        for (String selector : selectores) {
            Elements encontrados = doc.select(selector);
            if (!encontrados.isEmpty()) {
                resultados.addAll(encontrados);
                break;
            }
        }
        return resultados;
    }

    private Map<String, String> extraerDatos(Element elemento, ConfiguracionSitio config) {
        Map<String, String> datos = new HashMap<>();
        
        System.out.println("=== Procesando " + config.nombre + " ===");
    System.out.println("HTML recibido: " + elemento.outerHtml().substring(0, Math.min(200, elemento.outerHtml().length())));
        
        if (config.nombre.equals("Wallpaper Waves")) {
            Element titleElement = elemento.selectFirst("h3.jeg_post_title a");
            String titulo = (titleElement != null) ? titleElement.text() : "Wallpaper Waves";
            String enlace = (titleElement != null) ? titleElement.attr("href") : "";
            
            Element img = elemento.selectFirst(".jeg_thumb img");
            String preview = (img != null) ? img.attr("src") : "";
            
            datos.put("titulo", titulo);
            datos.put("enlace", enlace);
            datos.put("preview", preview);
            datos.put("tipo", "WallpaperWaves");
    }
        else if (config.nombre.equals("Motion Backgrounds")) {

         String titulo = elemento.attr("title");
    if (titulo.contains(" live wallpaper download")) {
        titulo = titulo.replace(" live wallpaper download", "");
    }
    
    if (titulo.isEmpty()) {
        titulo = elemento.text();
    }
    
    String enlace = elemento.attr("href");
    if (!enlace.startsWith("http")) {
        enlace = "https://motionbgs.com" + enlace;
    }
    
    datos.put("titulo", titulo.trim());
    datos.put("enlace", enlace);
    }

        else if (config.nombre.equals("Wallhaven")){
            
            // Elemento ahora es <figure>, podemos acceder a sus hijos
            Element linkElement = elemento.selectFirst("a.preview");
            String enlace = (linkElement != null) ? linkElement.attr("href") : "";
            
            if (!enlace.startsWith("http") && !enlace.isEmpty()) {
                enlace = "https://wallhaven.cc" + (enlace.startsWith("/") ? enlace : "/" + enlace);
            }

            // Extraer ID directamente del atributo del figure
            String wallpaperId = elemento.attr("data-wallpaper-id");
            
            // Extraer imagen (Wallhaven usa lazy loading con data-src)
            Element img = elemento.selectFirst("img");
            String preview = (img != null) ? img.attr("data-src") : "";
            if (preview.isEmpty() && img != null) preview = img.attr("src");
            
            // Extraer resolución
            Element resInfo = elemento.selectFirst(".wall-res");
            String resolucion = (resInfo != null) ? resInfo.text() : "";
            
            datos.put("titulo", "Wallhaven " + wallpaperId);
            datos.put("enlace", enlace);
            datos.put("preview", preview);
            datos.put("resolucion", resolucion);
            datos.put("tipo", "Wallhaven");
        }
        
        else if (config.nombre.equals("Wallpaper Flare")){
             Element figcaption = elemento.selectFirst("figcaption");
    String titulo = (figcaption != null) ? figcaption.text() : "";
    
    Element link = elemento.selectFirst("a[itemprop='url']");
    String enlace = (link != null) ? link.attr("href") : "";
    
    Element img = elemento.selectFirst("img");
    String preview = (img != null) ? img.attr("src") : "";
    if (preview.isEmpty() && img != null) {
        preview = img.attr("data-src");
    }
    
    String resolucion = "";
    if (img != null) {
        String alt = img.attr("alt");
        String title = img.attr("title");
        String[] textos = {alt, title};
        for (String texto : textos) {
            java.util.regex.Matcher matcher = 
                java.util.regex.Pattern.compile("\\d{3,4}x\\d{3,4}").matcher(texto);
            if (matcher.find()) {
                resolucion = matcher.group();
                break;
            }
        }
    }
    
    datos.put("titulo", titulo.isEmpty() ? "WallpaperFlare" : titulo);
    datos.put("enlace", enlace);
    datos.put("preview", preview);
    datos.put("resolucion", resolucion);
    datos.put("tipo", "WallpaperFlare");
        }
        
        else if (config.nombre.equals("Artvee")){
            Element titleElement = elemento.selectFirst("h3.product-title");
    String titulo = "";
    String enlace = "";
    
    if (titleElement != null) {
        Element link = titleElement.selectFirst("a");
        if (link != null) {
            titulo = link.text().trim();
            enlace = link.attr("href");
        }
    }
   
    if (titulo.isEmpty()) {
        Element anyLink = elemento.selectFirst("a");
        if (anyLink != null) {
            titulo = anyLink.text().trim();
            enlace = anyLink.attr("href");
        }
    }

    String info = "";
    
    Elements spans = elemento.select("span");
    for (Element span : spans) {
        String text = span.text();
        if (text.matches(".*\\d+\\s*x\\s*\\d+.*")) {
            info = text;
            break;
        }
    }
    
    if (info.isEmpty()) {
        String allText = elemento.text();
        java.util.regex.Matcher matcher = 
            java.util.regex.Pattern.compile("\\d+\\s*x\\s*\\d+\\s*px").matcher(allText);
        if (matcher.find()) {
            info = matcher.group();
        }
    }
    
    if (titulo.length() > 60) {
        titulo = titulo.substring(0, 57) + "...";
    }
    
    datos.put("titulo", titulo.isEmpty() ? "Artvee Artwork" : titulo);
    datos.put("enlace", enlace);
    datos.put("info", info);
    datos.put("tipo", "Artvee - Fine Art");
        }
        
        else if (config.nombre.equals("Moe Walls")){
    String titulo = elemento.text();
    
    String enlace = elemento.attr("href");
    
    String info = "Moe Walls Anime";
    
    datos.put("titulo", titulo);
    datos.put("enlace", enlace);
    datos.put("info", info); 

        }
        
        else if (config.nombre.equals("Wallpaper Abyss")) {
            String enlace = elemento.attr("href");
            if (!enlace.startsWith("http")) {
                enlace = "https://wall.alphacoders.com/" + enlace;
            }
            String titulo = elemento.attr("title");
            if (titulo.isEmpty()) {
                Element img = elemento.selectFirst("img");
                if (img != null) titulo = img.attr("alt");
            }
            datos.put("titulo", titulo.isEmpty() ? "Wallpaper Abyss" : titulo);
            datos.put("enlace", enlace);
            datos.put("tipo", "Wallpaper Abyss");
        }
        
        else if (config.nombre.equals("Peakpx")) {
            String enlace = elemento.attr("href");
            if (!enlace.startsWith("http")) {
                enlace = "https://www.peakpx.com" + enlace;
            }
            Element img = elemento.selectFirst("img");
            String titulo = (img != null) ? img.attr("alt") : "Peakpx Wallpaper";
            
            datos.put("titulo", titulo);
            datos.put("enlace", enlace);
            datos.put("tipo", "Peakpx");
        }
        
        return datos;
    }
    
 
    public void buscarEnMultiplesSitios(String busqueda, JTable tablaResultados) {
   
        DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"#", "Sitio", "Título", "Enlace", "Info"}, 
            0
        );
        tablaResultados.setModel(modelo);
        
        int contador = 1;
        
        for (ConfiguracionSitio config : sitiosConfigurados) {
            try {
                String url = construirURLCorrecta(config, busqueda);
                System.out.println("Buscando en " + config.nombre + ": " + url);
                
                Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();
                
            
                Elements elementos = buscarElementos(doc, config.selectores);
                System.out.println("  Encontrados: " + elementos.size());
                
          
                for (Element elemento : elementos) {
                    Map<String, String> datos = extraerDatos(elemento, config);
                    
                    modelo.addRow(new Object[]{
                        contador++,
                        config.nombre,
                        datos.get("titulo"),
                        datos.get("enlace"),
                        datos.get("resolucion")
                    });
                }
                
                Thread.sleep(2000);
                
            } catch (Exception e) {
                modelo.addRow(new Object[]{
                    contador++, 
                    config.nombre, 
                    "ERROR", 
                    e.getMessage(), 
                    ""
                });
            }
        }
    }

    /**
     * Método optimizado para uso Web/API que retorna una lista de datos.
     */
    public List<Map<String, String>> buscarWeb(String busqueda) {
        List<Map<String, String>> resultados = new ArrayList<>();
        
        for (ConfiguracionSitio config : sitiosConfigurados) {
            try {
                String url = construirURLCorrecta(config, busqueda);
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0")
                        .timeout(10000)
                        .get();

                Elements elementos = buscarElementos(doc, config.selectores);

                for (Element elemento : elementos) {
                    Map<String, String> datos = extraerDatos(elemento, config);
                    datos.put("sitio", config.nombre); // Agregamos el origen
                    resultados.add(datos);
                }
            } catch (Exception e) {
                System.out.println("Error scraping web " + config.nombre + ": " + e.getMessage());
            }
        }
        return resultados;
    }
}
    
    
