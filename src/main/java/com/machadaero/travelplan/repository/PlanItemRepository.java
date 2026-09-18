package com.machadaero.travelplan.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.machadaero.travelplan.entity.PlanItem;
import com.machadaero.travelplan.entity.TravelPlan;

@Repository
public interface PlanItemRepository extends JpaRepository<PlanItem, Long> {

    List<PlanItem> findByTravelPlanOrderBySortOrderAsc(TravelPlan travelPlan);

}
