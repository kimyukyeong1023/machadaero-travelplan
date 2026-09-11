package com.machadaero.travelplan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller 
public class mainController {
    @GetMapping("/")
    public String main() {
        
        return "index";
    }
    
    
}
