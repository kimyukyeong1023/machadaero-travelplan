package com.machadaero.travelplan.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.machadaero.travelplan.dto.PlanCreateRequestDto;
import com.machadaero.travelplan.dto.PlanResponseDto;
import com.machadaero.travelplan.service.PlanService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }
    
    @GetMapping("/plans")
    public String plans(HttpServletRequest request, Model model) {
        System.out.println("PlanController - plans()");

        HttpSession session = request.getSession(false);
        if (session == null) {
            return "redirect:/login";

        }
        // 세션에서 값을 꺼낼 때 무조건 가장 넓은 범위인 Object 타입으로 반환하기 때문에
        // 세션에서 값을 꺼낼 때는 처음에 넣었던 타입 그대로 형변환해 꺼내야 합니다.
        Long loginUserId = (Long) session.getAttribute("loginUserId");
        List<PlanResponseDto> planResponseDtoList=planService.getPlans(loginUserId);
        model.addAttribute("planList", planResponseDtoList);
        return "plans.html";
    }

    @GetMapping("/plans/new")
    public String creatPlan(HttpServletRequest request) {
        System.out.println("PlanController - createPlan()");

        HttpSession session = request.getSession(false);
        if (session == null) {
            return "redirect:/login";

        }
        // 세션에서 값을 꺼낼 때 무조건 가장 넓은 범위인 Object 타입으로 반환하기 때문에
        // 세션에서 값을 꺼낼 때는 처음에 넣었던 타입 그대로 형변환해 꺼내야 합니다.
        Long loginUserId = (Long) session.getAttribute("loginUserId");
        return "planNew";

    }

    @GetMapping("/plans/{planId}")
    public String planDetail(@PathVariable("planId") Long planId,
            HttpServletRequest request, Model model) {
        System.out.println("PlanController - planDetail()");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loginUserId") == null) {
            return "redirect:/login";
        }

        model.addAttribute("planId", planId);
        return "planDetail";
    }

    @PostMapping("/plans")
    public String savePlan(@ModelAttribute PlanCreateRequestDto requestDto, HttpServletRequest request) {
        // 1. 로그인 세션 확인 (없으면 로그인 페이지로)
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loginUserId") == null) {
            return "redirect:/login";
        }
        System.out.println("요청 dto: " + requestDto);
        Long loginUserId = (Long) session.getAttribute("loginUserId");
        planService.createPlan(loginUserId,requestDto);
        return "redirect:/plans";
    }

}
