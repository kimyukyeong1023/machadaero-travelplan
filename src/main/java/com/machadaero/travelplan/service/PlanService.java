package com.machadaero.travelplan.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.machadaero.travelplan.dto.PlanCreateRequestDto;
import com.machadaero.travelplan.dto.PlanResponseDto;
import com.machadaero.travelplan.dto.PlanUpdateRequestDto;
import com.machadaero.travelplan.entity.TravelPlan;
import com.machadaero.travelplan.entity.User;
import com.machadaero.travelplan.repository.TravelPlanRepository;
import com.machadaero.travelplan.repository.UserRepository;

@Service
public class PlanService {

    private final TravelPlanRepository travelPlanRepository;
    private final UserRepository userRepository;

    public PlanService(TravelPlanRepository travelPlanRepository, UserRepository userRepository) {
        this.travelPlanRepository = travelPlanRepository;
        this.userRepository = userRepository;
    }

    public void createPlan(Long loginUserId, PlanCreateRequestDto requestDto) {
        System.out.println("PlanService - createPlan()");

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

    public List<PlanResponseDto> getPlans(Long userId) {
        System.out.println("PlanService - getPlans()");
        List<PlanResponseDto> planList = new ArrayList<>();

        List<TravelPlan> travelPlans = travelPlanRepository.findAllByUser_Id(userId);
        for (TravelPlan travelPlan : travelPlans) {
            planList.add(new PlanResponseDto(travelPlan));
        }
        return planList;
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
        travelPlan.setTitle(requestDto.getTitle());
        travelPlan.setStartDate(requestDto.getStartDate());
        travelPlan.setEndDate(requestDto.getEndDate());

        travelPlanRepository.save(travelPlan);
    }

    public void deletePlan(Long loginUserId, Long planId) {
        System.out.println("PlanService - deletePlan()");

        TravelPlan travelPlan = travelPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("여행계획을 찾을 수 없습니다."));

        if (loginUserId == null || !loginUserId.equals(travelPlan.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 여행계획만 삭제할 수 있습니다.");
        }

        travelPlanRepository.delete(travelPlan);
    }
}
