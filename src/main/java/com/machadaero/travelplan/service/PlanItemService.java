package com.machadaero.travelplan.service;

import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.machadaero.travelplan.dto.PlanItemOrderRequestDto;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.machadaero.travelplan.dto.PlanItemCreateRequestDto;
import com.machadaero.travelplan.dto.PlanItemUpdateRequestDto;
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

        // Codex 수정: 계획 소유자를 확인하고 마지막 순서에 새 일정을 추가합니다.
@Transactional
public PlanItem createPlanItem(
        Long loginUserId, Long planId, PlanItemCreateRequestDto requestDto) {

    System.out.println("PlanItemService - createPlanItem()");

    if (loginUserId == null) {
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
    }

    // Codex 수정: 순서 저장과 같은 계획 잠금을 사용합니다.
    // 동시에 추가 요청이 들어와도 같은 마지막 순번을 사용하지 않게 합니다.
    TravelPlan travelPlan = travelPlanRepository.findByIdForOrderUpdate(planId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "여행계획이 없습니다."));

    if (!loginUserId.equals(travelPlan.getUser().getId())) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "본인의 여행계획에만 추가할 수 있습니다.");
    }

    if (requestDto == null
            || requestDto.getPlaceName() == null
            || requestDto.getPlaceName().isBlank()) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "장소명이 필요합니다.");
    }

    PlanItem probe = new PlanItem();
    probe.setTravelPlan(travelPlan);

    int nextSortOrder = planItemRepository.findAll(
                    Example.of(probe),
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

        public List<PlanItem> getPlanItems(Long loginUserId, Long planId) {
                System.out.println("PlanItemService - getPlanItems");

                TravelPlan travelPlan = travelPlanRepository.findById(planId)
                                .orElseThrow(() -> new IllegalArgumentException("여행계획이 없습니다."));

                if (!loginUserId.equals(travelPlan.getUser().getId())) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "본인의 여행계획만 조회할 수 있습니다.");
                }
                List<PlanItem> planList = planItemRepository.findByTravelPlanOrderBySortOrderAsc(travelPlan);
                return planList;

        }

        // Codex 작성: 전체 순서를 검증한 뒤 1, 2, 3...으로 한 트랜잭션에서 저장합니다.
        @Transactional
        public void reorderPlanItems(Long loginUserId, Long planId, PlanItemOrderRequestDto requestDto) {
                System.out.println("PlanItemService - reorderPlanItems()");
                if (loginUserId == null) {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
                }
                TravelPlan travelPlan = travelPlanRepository.findByIdForOrderUpdate(planId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "여행계획이 없습니다."));
                if (!loginUserId.equals(travelPlan.getUser().getId())) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 여행계획만 수정할 수 있습니다.");
                }
                List<PlanItem> items = planItemRepository.findByTravelPlanOrderBySortOrderAsc(travelPlan);
                List<Long> currentIds = items.stream().map(PlanItem::getId).toList();
                if (requestDto == null || requestDto.getItemIds() == null || requestDto.getOriginalItemIds() == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "일정 순서 목록이 필요합니다.");
                }


                List<Long> requestedIds = requestDto.getItemIds();
                if (requestedIds.size() != currentIds.size()
                                || new HashSet<>(requestedIds).size() != requestedIds.size()
                                || !new HashSet<>(requestedIds).equals(new HashSet<>(currentIds))) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 계획의 모든 일정을 중복 없이 보내야 합니다.");
                }
                
                // Codex 작성: 검증을 모두 통과한 후에만 순번을 변경합니다. 제목·날짜·메모는 변경하지 않습니다.
                Map<Long, PlanItem> itemsById = items.stream()
                                .collect(Collectors.toMap(PlanItem::getId, Function.identity()));
                for (int index = 0; index < requestedIds.size(); index++) {
                        itemsById.get(requestedIds.get(index)).setSortOrder(index + 1);
                }
                planItemRepository.saveAll(items);
        }

        @Transactional
        public void updatePlanItem(Long loginUserId, Long planId, Long itemId, PlanItemUpdateRequestDto requestDto) {
                System.out.println("PlanItemService - updatePlanItem");

                TravelPlan travelPlan = travelPlanRepository.findById(planId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "여행계획이 없습니다."));

                if (loginUserId == null || !loginUserId.equals(travelPlan.getUser().getId())) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 여행계획만 수정할 수 있습니다.");
                }

                PlanItem planItem = planItemRepository.findById(itemId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "일정이 없습니다."));

                if (!planId.equals(planItem.getTravelPlan().getId())) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 여행계획의 일정이 아닙니다.");
                }

                planItem.setPlaceName(requestDto.getPlaceName());
                planItem.setAddress(requestDto.getPlaceAddress());
                planItem.setVisitDate(requestDto.getVisitDate());
                planItem.setVisitTime(requestDto.getVisitTime());
                planItem.setDuration(requestDto.getDuration());
                planItem.setMemo(requestDto.getMemo());
                planItem.setExtraMemo(requestDto.getExtraMemo());

                planItemRepository.save(planItem);
        }

        @Transactional
        public void deletePlanItem(Long loginUserId, Long planId, Long itemId) {
                System.out.println("PlanItemService - deletePlanItem()");

                TravelPlan travelPlan = travelPlanRepository.findById(planId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "여행계획이 없습니다."));

                if (loginUserId == null || !loginUserId.equals(travelPlan.getUser().getId())) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 여행계획의 일정만 삭제할 수 있습니다.");
                }

                PlanItem planItem = planItemRepository.findById(itemId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "일정이 없습니다."));

                if (!planId.equals(planItem.getTravelPlan().getId())) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 여행계획의 일정이 아닙니다.");
                }

                planItemRepository.delete(planItem);
        }

}
