package com.persepolis.IA.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class SpaController {

    // Redirige rutas que no son archivos (sin punto) al index.html
    // Esto maneja el routing del lado del cliente (SPA) antes de que sea un error 404
    @RequestMapping(value = "/{path:[^\\.]*}")
    public String redirect(@PathVariable String path) {
        // Evitar bucle infinito: Si la ruta es "error", no redirigir al index
        if ("error".equals(path)) {
            return "forward:/error_page"; 
        }
        return "forward:/index.html";
    }
}