package com.machadaero.travelplan.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.machadaero.travelplan.dto.PlanCreateRequestDto;
import com.machadaero.travelplan.dto.PlanResponseDto;
import com.machadaero.travelplan.dto.PlanUpdateRequestDto;
import com.machadaero.travelplan.entity.PlanItem;
import com.machadaero.travelplan.service.PlanItemService;
import com.machadaero.travelplan.service.PlanService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class PlanController {

    private final PlanService planService;
    private final PlanItemService planItemService;

    public PlanController(PlanService planService, PlanItemService planItemService) {
        this.planService = planService;
        this.planItemService = planItemService;
    }

    @GetMapping("/plans")
    // Codex 수정: 전체 계획을 한 번 전달하고, 분류·정렬·페이지 이동은 브라우저에서 처리합니다.
    public String plans(HttpServletRequest request, Model model) {
        System.out.println("PlanController - plans()");

        HttpSession session = request.getSession(false);
        if (session == null) {
            return "redirect:/login";

        }
        // 세션에서 값을 꺼낼 때 무조건 가장 넓은 범위인 Object 타입으로 반환하기 때문에
        // 세션에서 값을 꺼낼 때는 처음에 넣었던 타입 그대로 형변환해 꺼내야 합니다.
        Long loginUserId = (Long) session.getAttribute("loginUserId");
        if (loginUserId == null) {
            return "redirect:/login";
        }

        List<PlanResponseDto> plans = planService.getPlans(loginUserId);
        model.addAttribute("planList", plans);
        return "plans.html";
    }

    @GetMapping("/plans/new")
    public String showCreatPlan(HttpServletRequest request) {
        System.out.println("PlanController - showCreatPlan()");

        HttpSession session = request.getSession(false);
        if (session == null) {
            return "redirect:/login";
        }
        Long loginUserId = (Long) session.getAttribute("loginUserId");
        if (loginUserId == null) {
            return "redirect:/login";
        }

        return "planNew";

    }

    @GetMapping("/plans/{planId}")
    public String PlanDetail(@PathVariable("planId") Long planId,
            HttpServletRequest request, Model model) {
        System.out.println("PlanController - PlanDetail()");

        HttpSession session = request.getSession(false);
        if (session == null) {
            return "redirect:/login";
        }
        Long loginUserId = (Long) session.getAttribute("loginUserId");
        if (loginUserId == null) {
            return "redirect:/login";
        }

        model.addAttribute("planId", planId);

        List<PlanItem> planItemList = planItemService.getPlanItems(loginUserId, planId);

        model.addAttribute("planItemList", planItemList);
        return "planDetail";
    }

    @PostMapping("/plans")
    public String savePlan(@ModelAttribute PlanCreateRequestDto requestDto,
            HttpServletRequest request, Model model) {
        System.out.println("PlanController - savePlan()");

        // 1. 로그인 세션 확인 (없으면 로그인 페이지로)
        HttpSession session = request.getSession(false);
        if (session == null) {
            return "redirect:/login";
        }
        Long loginUserId = (Long) session.getAttribute("loginUserId");
        if (loginUserId == null) {
            return "redirect:/login";
        }

        System.out.println("요청 dto: " + requestDto);

        try {
            planService.createPlan(loginUserId, requestDto);
        } catch (IllegalArgumentException e) {
            model.addAttribute("formData", requestDto);
            model.addAttribute("errorMessage", e.getMessage());
            return "planNew";
        }
        return "redirect:/plans";
    }

    @GetMapping("/plans/{planId}/edit")
    public String showPlanEditForm(
            @PathVariable("planId") Long planId,
            HttpServletRequest request,
            Model model) {
        System.out.println("PlanController - showPlanEditForm()");

        HttpSession session = request.getSession(false);
        if (session == null) {
            return "redirect:/login";
        }
        Long loginUserId = (Long) session.getAttribute("loginUserId");
        if (loginUserId == null) {
            return "redirect:/login";
        }

        PlanResponseDto planResponseDto = planService.getPlan(loginUserId, planId);

        model.addAttribute("planDto", planResponseDto);
        // Codex 수정: 본인 계획의 일정을 기존 순서대로 조회하여 수정 화면의 읽기 전용 목록에 전달합니다.
        model.addAttribute("planItemList", planItemService.getPlanItems(loginUserId, planId));

        return "planNew";
    }

    @PostMapping("/plans/{planId}/edit")
    public String updatePlan(
            @PathVariable("planId") Long planId,
            @ModelAttribute PlanUpdateRequestDto requestDto,
            HttpServletRequest request, Model model) {
        System.out.println("PlanController - updatePlan()");

        HttpSession session = request.getSession(false);
        if (session == null) {
            return "redirect:/login";
        }
        Long loginUserId = (Long) session.getAttribute("loginUserId");
        if (loginUserId == null) {
            return "redirect:/login";
        }

        // 유저가 다른 유저의 계획을 수정하지 못하게 하려면 유저 아이디도 넘겨서 검사.
        try {
            planService.updatePlan(planId, loginUserId, requestDto);
        } catch (IllegalArgumentException e) {
            model.addAttribute("planDto", planService.getPlan(loginUserId, planId));
            // Codex 수정: 날짜 검증 실패로 수정 화면을 다시 열어도 기존 일정 목록을 함께 표시합니다.
            model.addAttribute("planItemList", planItemService.getPlanItems(loginUserId, planId));
            model.addAttribute("formData", requestDto);
            model.addAttribute("errorMessage", e.getMessage());
            return "planNew";
        }

        return "redirect:/plans";
    }

    @PostMapping("/plans/{planId}/delete")
    public String deletePlan(@PathVariable("planId") Long planId,
            HttpServletRequest request) {
        System.out.println("PlanController - deletePlan()");

        HttpSession session = request.getSession(false);
        if (session == null) {
            return "redirect:/login";
        }
        Long loginUserId = (Long) session.getAttribute("loginUserId");
        if (loginUserId == null) {
            return "redirect:/login";
        }
        
        planService.deletePlan(loginUserId, planId);

        return "redirect:/plans";
    }

}
