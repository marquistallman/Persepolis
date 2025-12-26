
package com.persepolis.IA.Scraper;


public class ConfiguracionSitio {
    
    String nombre; 
    String urlBase; 
    String parametroBusqueda; 
    String rutaBusqueda; 
    String[] selectores; 

    public ConfiguracionSitio(String nombre, String urlBase, String parametroBusqueda, String rutaBusqueda, String[] selectores) {
        this.nombre = nombre;
        this.urlBase = urlBase;
        this.parametroBusqueda = parametroBusqueda;
        this.rutaBusqueda = rutaBusqueda;
        this.selectores = selectores;
    }
    
    
    
}
