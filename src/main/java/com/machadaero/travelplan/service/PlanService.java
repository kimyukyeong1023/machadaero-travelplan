package com.machadaero.travelplan.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.machadaero.travelplan.dto.PlanCreateRequestDto;
import com.machadaero.travelplan.dto.PlanResponseDto;
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


        TravelPlan travelPlan= TravelPlan.builder()
                                        .user(user)
                                        .title(requestDto.getTitle())
                                        .startDate(requestDto.getStartDate())
                                        .endDate(requestDto.getEndDate())
                                        .build();
        travelPlanRepository.save(travelPlan);

        System.out.println("DB 저장 완료! 생성된 계획 ID: " + travelPlan.getId());
    }

    public List<PlanResponseDto> getPlans(Long userId){
        System.out.println("PlanService - getPlans()");
        List<PlanResponseDto> planList= new ArrayList<>();

        List<TravelPlan> travelPlans = travelPlanRepository.findAllByUser_Id(userId);
        for(TravelPlan travelPlan:travelPlans){
            planList.add(new PlanResponseDto(travelPlan));
        }
        return planList;
    }
}
