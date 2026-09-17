package com.machadaero.travelplan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.machadaero.travelplan.entity.PlanItem;

@Repository
public interface PlanItemRepository extends JpaRepository<PlanItem, Long> {
}
