package com.machadaero.travelplan.service;

import java.util.List;

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

        @Transactional
        public PlanItem createPlanItem(Long planId, PlanItemCreateRequestDto requestDto) {
                System.out.println("PlanItemService - createPlanItem()");

                TravelPlan travelPlan = travelPlanRepository.findById(planId)
                                .orElseThrow(() -> new IllegalArgumentException("여행계획이 없습니다."));

                // 해당 여행계획의 마지막 일정 순서를 조회합니다. 첫 일정은 1번입니다.
                // DB에서 이미 순번이 가장 큰 일정 1개를 골라서 서버로 가져오고,
                // 스트림에서는 그 객체의 순번 값을 꺼내는 거
                PlanItem probe = new PlanItem();
                probe.setTravelPlan(travelPlan);
                // travelPlan에 조회한 계획의 참조를 넣어서 검색 조건을 만드는 거
                int nextSortOrder = planItemRepository.findAll(Example.of(probe),
                                // 첫 번째 페이지, 한 페이지에 일정 1개, 순번이 큰 것부터 정렬
                                // findAll(...)이라는 이름이어도 모든 일정을 자바로 가져오는 게 아니라,
                                // 조건에 맞는 일정 중 순번이 가장 큰 1개를 요청
                                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "sortOrder")))
                                .getContent().stream() // 조회된 일정 목록: 0개 또는 1개, 목록을 스트림으로 처리
                                .map(PlanItem::getSortOrder)// 일정 객체에서 순번을 꺼냄
                                // 위와 같은 의미 .map(item -> item.getSortOrder())
                                .findFirst()
                                .orElse(0) + 1; // 없으면 0 + 1, 있으면 해당 순번 + 1

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
