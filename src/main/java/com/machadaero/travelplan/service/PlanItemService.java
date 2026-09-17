package com.machadaero.travelplan.service;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.machadaero.travelplan.dto.PlanItemCreateRequestDto;
import com.machadaero.travelplan.entity.PlanItem;
import com.machadaero.travelplan.entity.TravelPlan;
import com.machadaero.travelplan.repository.PlanItemRepository;
import com.machadaero.travelplan.repository.TravelPlanRepository;

@Service
public class PlanItemService {

    private final PlanItemRepository planItemRepository;
    private final TravelPlanRepository travelPlanRepository;

    public PlanItemService(PlanItemRepository planItemRepository,
            TravelPlanRepository travelPlanRepository) {
        this.planItemRepository = planItemRepository;
        this.travelPlanRepository = travelPlanRepository;
    }

    @Transactional
    public PlanItem createPlanItem(Long planId, PlanItemCreateRequestDto requestDto) {
        System.out.println("PlanItemService - createPlanItem()");

        TravelPlan travelPlan = travelPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("여행계획이 없습니다."));

        // 해당 여행계획의 마지막 일정 순서를 조회합니다. 첫 일정은 1번입니다.
        PlanItem probe = new PlanItem();
        probe.setTravelPlan(travelPlan);
        int nextSortOrder = planItemRepository.findAll(Example.of(probe),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "sortOrder")))
                .getContent().stream()
                .map(PlanItem::getSortOrder)
                .findFirst()
                .orElse(0) + 1;

        PlanItem planItem = PlanItem.builder()
                .requestDto(requestDto)
                .build();
        planItem.setTravelPlan(travelPlan);
        planItem.setSortOrder(nextSortOrder);

        return planItemRepository.save(planItem);
    }
}
