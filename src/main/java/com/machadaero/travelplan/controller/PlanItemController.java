package com.machadaero.travelplan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;
import com.machadaero.travelplan.dto.PlanItemOrderRequestDto;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.machadaero.travelplan.dto.HomePlanItemResponseDto;
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

    // Codex 작성: 순서 저장 버튼의 JSON 요청만 처리하며, 세션 만료 시 JS가 알 수 있도록 401을 반환합니다.
    @PostMapping("/plans/{planId}/items/order")
    public ResponseEntity<Void> reorderPlanItems(@PathVariable("planId") Long planId,
            @RequestBody PlanItemOrderRequestDto requestDto, HttpServletRequest request) {
        System.out.println("PlanItemController - reorderPlanItems()");
        HttpSession session = request.getSession(false);
        Long loginUserId = session == null ? null : (Long) session.getAttribute("loginUserId");
        if (loginUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        planItemService.reorderPlanItems(loginUserId, planId, requestDto);
        return ResponseEntity.noContent().build();
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

        // Codex 수정: 기존 상세 페이지의 일정 추가에도 로그인 사용자 ID를 전달합니다.
        PlanItem planItem = planItemService.createPlanItem(loginUserId, planId, requestDto);
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

    // Codex 추가: 메인에서 관광지 이름·주소를 받아 새 일정을 저장합니다.
    // 화면 이동 없이 생성한 일정 DTO를 JSON으로 반환합니다.
    @PostMapping("/api/plans/{planId}/items")
    public ResponseEntity<HomePlanItemResponseDto> createHomePlanItem(
            @PathVariable("planId") Long planId,
            @RequestBody PlanItemCreateRequestDto requestDto,
            HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        Long loginUserId = session == null
                ? null
                : (Long) session.getAttribute("loginUserId");

        if (loginUserId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        PlanItem item = planItemService.createPlanItem(loginUserId, planId, requestDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new HomePlanItemResponseDto(item));
    }

    // Codex 추가: 메인 오른쪽 영역에서 화면 이동 없이 일정을 삭제합니다.
    @DeleteMapping("/api/plans/{planId}/items/{itemId}")
    public ResponseEntity<Void> deleteHomePlanItem(
            @PathVariable("planId") Long planId,
            @PathVariable("itemId") Long itemId,
            HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        Long loginUserId = session == null
                ? null
                : (Long) session.getAttribute("loginUserId");

        if (loginUserId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        // Codex 추가: 기존 서비스의 계획 소유자·일정 소속 확인을 재사용합니다.
        planItemService.deletePlanItem(loginUserId, planId, itemId);

        return ResponseEntity.noContent().build();
    }
}
