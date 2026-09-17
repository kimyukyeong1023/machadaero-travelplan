package com.machadaero.travelplan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;


@Controller 
public class PlanController {
    @GetMapping("/plans")
    public String plans(HttpServletRequest request) {
        HttpSession session= request.getSession(false);
        if (session==null) {
            return "redirect:/login";
            
        }
        String loginUserId=(String)session.getAttribute("loginUserId");
        return "";
    }
    
    
}
