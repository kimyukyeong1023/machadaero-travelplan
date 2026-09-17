package com.machadaero.travelplan.dto;
import lombok.Getter;
import java.time.LocalDate;

import com.machadaero.travelplan.entity.TravelPlan;

@Getter
public class PlanResponseDto {
    private Long id;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;

    // Entity를 받아서 DTO로 변환하는 생성자
    public PlanResponseDto(TravelPlan entity) {
        this.id = entity.getId();
        this.title = entity.getTitle();
        this.startDate = entity.getStartDate();
        this.endDate = entity.getEndDate();
    }
}