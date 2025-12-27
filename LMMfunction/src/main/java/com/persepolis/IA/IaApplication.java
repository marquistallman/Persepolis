package com.persepolis.IA;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.File;
import java.io.IOException;

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

			startFrontend();
		};
	}

	private void startFrontend() {
		try {
			// Buscamos dinámicamente la carpeta que contenga package.json
			// Probamos rutas relativas para cuando se corre desde 'LMMfunction' o desde 'WallPaperSystem'
			String[] possiblePaths = { "../front/Front", "front/Front", "../front", "front" };
			File frontendDir = null;

			for (String path : possiblePaths) {
				File dir = new File(path);
				// Verificamos que exista el directorio Y que tenga el package.json dentro
				if (dir.exists() && dir.isDirectory() && new File(dir, "package.json").exists()) {
					frontendDir = dir;
					break;
				}
			}

			if (frontendDir != null) {
				System.out.println("--- Frontend encontrado en: " + frontendDir.getCanonicalPath() + " ---");
				// Usamos 'cmd /k' para mantener la ventana abierta y asegurar un entorno de shell correcto
				// "\"WallPaperSystem\"" es el título de la ventana (requerido por start si hay comillas)
				// Comando: Si no hay node_modules instala, luego abre el index.html y corre el dev script
				new ProcessBuilder("cmd", "/c", "start", "\"WallPaperSystem\"", "cmd", "/k", "if not exist node_modules (echo Instalando dependencias... && call npm install) && start http://localhost:8080 && npm run dev")
						.directory(frontendDir)
						.start();
			} else {
				System.out.println("--- ADVERTENCIA: No se encontró 'package.json' en ninguna ruta esperada. ---");
				System.out.println("--- Directorio actual de ejecución: " + new File(".").getCanonicalPath());
			}
		} catch (IOException e) {
			System.out.println("Nota: No se pudo iniciar el frontend automáticamente (" + e.getMessage() + ")");
		}
	}
}
