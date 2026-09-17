package com.machadaero.travelplan.entity;

import java.time.LocalDate;
import java.time.LocalTime;

import com.machadaero.travelplan.dto.PlanItemCreateRequestDto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name = "plan_items")
@Entity
@Getter
@Setter
@NoArgsConstructor
public class PlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "travel_plan_id", nullable = false)
    private TravelPlan travelPlan;

    @ManyToOne
    @JoinColumn(name = "tourist_place_id")
    private TouristPlace touristPlace;

    private String placeName;

    private String address;

    // 저장 전에 서버에서 일정 표시 순서를 지정합니다.
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    private LocalDate visitDate;

    private LocalTime visitTime;

    private String memo;

    // 소요시간 (분 단위)
    private Integer duration;

    private String extraMemo;

    @Builder
    public PlanItem(PlanItemCreateRequestDto requestDto) {
        this.placeName = requestDto.getPlaceName();
        this.address = requestDto.getPlaceAddress();
        this.visitDate = requestDto.getVisitDate();
        this.visitTime = requestDto.getVisitTime();
        this.memo = requestDto.getMemo();
        this.duration = requestDto.getDuration();
        this.extraMemo = requestDto.getExtraMemo();
    }

    public PlanItem(TravelPlan travelPlan, TouristPlace touristPlace, String placeName,
            String address, Integer sortOrder, LocalDate visitDate, LocalTime visitTime,
            String memo, Integer duration, String extraMemo) {
        this.travelPlan = travelPlan;
        this.touristPlace = touristPlace;
        this.placeName = placeName;
        this.address = address;
        this.sortOrder = sortOrder;
        this.visitDate = visitDate;
        this.visitTime = visitTime;
        this.memo = memo;
        this.duration = duration;
        this.extraMemo = extraMemo;
    }
}
