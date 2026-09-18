package com.machadaero.travelplan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.machadaero.travelplan.dto.PlanItemCreateRequestDto;
import com.machadaero.travelplan.dto.PlanItemUpdateRequestDto;
import com.machadaero.travelplan.entity.PlanItem;
import com.machadaero.travelplan.service.PlanItemService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class PlanItemController {

    private final PlanItemService planItemService;

    public PlanItemController(PlanItemService planItemService) {
        this.planItemService = planItemService;
    }

    @PostMapping("/plans/{planId}/items")
    public String createPlanItem(@PathVariable("planId") Long planId,
            @ModelAttribute PlanItemCreateRequestDto requestDto,
            HttpServletRequest request) {
        System.out.println("PlanItemController - createPlanItem()");

        HttpSession session = request.getSession(false);
        if (session == null) {
            return "redirect:/login";
        }
        Long loginUserId = (Long) session.getAttribute("loginUserId");
        if (loginUserId == null) {
            return "redirect:/login";
        }

        PlanItem planItem = planItemService.createPlanItem(planId, requestDto);
        return "redirect:/plans/" + planId;
    }

    @GetMapping("/plans/{planId}/items/{itemId}/edit")
    public String showPlanItemEditForm(@PathVariable("planId") Long planId,
            @PathVariable("itemId") Long itemId,
            HttpServletRequest request) {
        System.out.println("PlanItemController - showPlanItemEditForm()");

        HttpSession session = request.getSession(false);
        if (session == null) {
            return "redirect:/login";
        }
        Long loginUserId = (Long) session.getAttribute("loginUserId");
        if (loginUserId == null) {
            return "redirect:/login";
        }

        

        return "redirect:/plans/" + planId;
    }

    @PostMapping("/plans/{planId}/items/{itemId}/edit")
    public String updatePlanItem(@PathVariable("planId") Long planId,
            @PathVariable("itemId") Long itemId,
            @ModelAttribute PlanItemUpdateRequestDto requestDto,
            HttpServletRequest request) {
        System.out.println("PlanItemController - updatePlanItem()");

        HttpSession session = request.getSession(false);
        if (session == null) {
            return "redirect:/login";
        }
        Long loginUserId = (Long) session.getAttribute("loginUserId");
        if (loginUserId == null) {
            return "redirect:/login";
        }
        planItemService.updatePlanItem(loginUserId, planId, itemId, requestDto);

        return "redirect:/plans/" + planId;
    }
    @PostMapping("/plans/{planId}/items/{itemId}/delete")
    public String deletePlanItem(@PathVariable("planId") Long planId,
            @PathVariable("itemId") Long itemId,
            HttpServletRequest request) {
        System.out.println("PlanItemController - deletePlanItem()");

        HttpSession session = request.getSession(false);
        if (session == null) {
            return "redirect:/login";
        }
        Long loginUserId = (Long) session.getAttribute("loginUserId");
        if (loginUserId == null) {
            return "redirect:/login";
        }

        planItemService.deletePlanItem(loginUserId, planId, itemId);
        return "redirect:/plans/" + planId;
    }
}
