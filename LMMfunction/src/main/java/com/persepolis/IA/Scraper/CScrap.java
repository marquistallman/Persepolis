package com.persepolis.IA.Scraper;

import com.persepolis.IA.Scraper.model.WallpaperDTO;
import com.persepolis.IA.Scraper.sitios.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;



public class CScrap {
    
    private List<SitioBase> sitios; 
    
    public CScrap(){
    
        inicializarSitios(); 
    
}
    
    private void inicializarSitios(){
        
        sitios = new ArrayList<>();
        sitios.add(new WallpaperWaves());
        sitios.add(new MotionBackgrounds());
        sitios.add(new MoeWalls());
        sitios.add(new Wallhaven());
        sitios.add(new WallpaperFlare());
        sitios.add(new Artvee());
        sitios.add(new WallpaperAbyss());
        sitios.add(new Peakpx());
    }
    
 
    public void buscarEnMultiplesSitios(String busqueda, JTable tablaResultados) {
   
        DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"#", "Sitio", "Título", "Enlace", "Info"}, 
            0
        );
        tablaResultados.setModel(modelo);
        
        int contador = 1;
        
        for (SitioBase sitio : sitios) {
            try {
                System.out.println("Buscando en " + sitio.getNombre() + "...");
                // Delegamos la búsqueda y gestión de memoria a la clase base
                List<WallpaperDTO> resultados = sitio.buscar(busqueda);
                System.out.println("  Encontrados: " + resultados.size());
                
                for (WallpaperDTO dto : resultados) {
                    modelo.addRow(new Object[]{
                        contador++,
                        sitio.getNombre(),
                        dto.getTitulo(),
                        dto.getEnlace(),
                        dto.getResolucion()
                    });
                }
                
            } catch (Exception e) {
                modelo.addRow(new Object[]{
                    contador++, 
                    sitio.getNombre(), 
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
    public List<WallpaperDTO> buscarWeb(String busqueda) {
        List<WallpaperDTO> resultados = new ArrayList<>();
        
        for (SitioBase sitio : sitios) {
            try {
                System.out.println("--- Scraping site: " + sitio.getNombre());
                List<WallpaperDTO> encontrados = sitio.buscar(busqueda);
                resultados.addAll(encontrados);
            } catch (Exception e) {
                System.out.println("Error scraping web " + sitio.getNombre() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        return resultados;
    }

    public WallpaperDTO obtenerDetalles(String url, String nombreSitio) {
        for (SitioBase sitio : sitios) {
            if (sitio.getNombre().equalsIgnoreCase(nombreSitio)) {
                return sitio.obtenerDetalles(url);
            }
        }
        return new WallpaperDTO();
    }
}
    
    
