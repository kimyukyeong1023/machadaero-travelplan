package com.machadaero.travelplan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.machadaero.travelplan.dto.PlanItemCreateRequestDto;
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
        if (session == null || session.getAttribute("loginUserId") == null) {
            return "redirect:/login";
        }

        PlanItem planItem = planItemService.createPlanItem(planId,requestDto);
        return "redirect:/plans/" + planId;
    }
}
