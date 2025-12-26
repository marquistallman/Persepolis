package com.persepolis.IA;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class IaApplication {

	public static void main(String[] args) {
		SpringApplication.run(IaApplication.class, args);
	}

	@Bean
	public CommandLineRunner init() {
		return args -> {
			System.out.println("--- Aplicación iniciada ---");
			System.out.println("Puerto: 8080");
			System.out.println("--- Rutas de Prueba (TestController) ---");
			System.out.println("Verificar Ollama: http://localhost:8080/test/ollama");
			System.out.println("Chat Interactivo: http://localhost:8080/test/chat?message=Hola");
			System.out.println("Historial Chat:   http://localhost:8080/test/chat/history");
			System.out.println("--- Rutas de Prueba (ScraperController) ---");
			System.out.println("Scraper:          http://localhost:8080/scraper");
		};
	}
}
