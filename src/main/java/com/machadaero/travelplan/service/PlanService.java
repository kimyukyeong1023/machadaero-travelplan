package com.machadaero.travelplan.service;

import java.time.LocalDate;
import java.util.List;


import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.machadaero.travelplan.dto.PlanCreateRequestDto;
import com.machadaero.travelplan.dto.PlanResponseDto;
import com.machadaero.travelplan.dto.PlanUpdateRequestDto;
import com.machadaero.travelplan.entity.TravelPlan;
import com.machadaero.travelplan.entity.User;
import com.machadaero.travelplan.repository.PlanItemRepository;
import com.machadaero.travelplan.repository.TravelPlanRepository;
import com.machadaero.travelplan.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class PlanService {

    private final TravelPlanRepository travelPlanRepository;
    private final PlanItemRepository planItemRepository;
    private final UserRepository userRepository;

    public PlanService(TravelPlanRepository travelPlanRepository, UserRepository userRepository, PlanItemRepository planItemRepository) {
        this.travelPlanRepository = travelPlanRepository;
        this.userRepository = userRepository;
        this.planItemRepository =planItemRepository;
    }

    public void createPlan(Long loginUserId, PlanCreateRequestDto requestDto) {
        System.out.println("PlanService - createPlan()");
        validateDates(requestDto.getStartDate(), requestDto.getEndDate());

        User user = userRepository.findById(loginUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        TravelPlan travelPlan = TravelPlan.builder()
                .user(user)
                .title(requestDto.getTitle())
                .startDate(requestDto.getStartDate())
                .endDate(requestDto.getEndDate())
                .build();
        travelPlanRepository.save(travelPlan);

        System.out.println("DB 저장 완료! 생성된 계획 ID: " + travelPlan.getId());
    }

    // Codex 수정: Page 대신 계획 DTO 전체 목록을 반환하고, 화면 처리는 plans.js에 맡깁니다.
    public List<PlanResponseDto> getPlans(Long userId) {
        // 사용자 ID로 조회하므로 다른 사용자의 계획은 포함되지 않습니다.
        List<TravelPlan> plans = travelPlanRepository.findAllByUser_Id(userId);
        // 계획의 ID·제목·날짜만 DTO로 변환합니다. PlanItem은 조회하지 않습니다.
        return plans.stream().map(PlanResponseDto::new).toList();
    }

    public PlanResponseDto getPlan(Long loginUserId, Long planId) {
        System.out.println("PlanService - getPlan()");

        TravelPlan travelPlan = travelPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("여행계획을 찾을 수 없습니다."));

        if (!loginUserId.equals(travelPlan.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 여행계획만 조회할 수 있습니다.");

        }
        return new PlanResponseDto(travelPlan);
    }

    public void updatePlan(Long planId, Long loginUserId, PlanUpdateRequestDto requestDto) {
        System.out.println("PlanService - updatePlan()");

        TravelPlan travelPlan = travelPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        if (!loginUserId.equals(travelPlan.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 계획만 조회하세요");
        }
        validateDates(requestDto.getStartDate(), requestDto.getEndDate());
        travelPlan.setTitle(requestDto.getTitle());
        travelPlan.setStartDate(requestDto.getStartDate());
        travelPlan.setEndDate(requestDto.getEndDate());

        travelPlanRepository.save(travelPlan);
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다.");
        }
    }

    @Transactional 
    public void deletePlan(Long loginUserId, Long planId) {
        System.out.println("PlanService - deletePlan()");

        TravelPlan travelPlan = travelPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("여행계획을 찾을 수 없습니다."));

        if (loginUserId == null || !loginUserId.equals(travelPlan.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 여행계획만 삭제할 수 있습니다.");
        }
        planItemRepository.deleteByTravelPlan(travelPlan);

        travelPlanRepository.delete(travelPlan);
    }
}
