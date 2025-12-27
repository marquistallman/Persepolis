package com.persepolis.IA.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import java.net.URI;

@RestController
public class HomeController {

    @GetMapping("/")
    public Mono<Void> home(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FOUND); // Código 302 para redirección
        // Ajustamos a 'Pages' (mayúscula) para coincidir con la carpeta física real
        response.getHeaders().setLocation(URI.create("/Pages/homepage.html"));
        return response.setComplete();
    }
}