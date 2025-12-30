package com.persepolis.IA.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        // Redirige al index compilado en /Pages
        return "redirect:/Pages/homepage.html";
    }
}