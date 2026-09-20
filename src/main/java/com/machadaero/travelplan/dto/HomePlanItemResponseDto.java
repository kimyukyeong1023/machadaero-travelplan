package com.machadaero.travelplan.dto;

import com.machadaero.travelplan.entity.PlanItem;
import lombok.Getter;

// Codex 수정: 메인 일정 표시와 순서 저장에 필요한 값을 반환합니다.
@Getter
public class HomePlanItemResponseDto {

    // Codex 추가: JavaScript에서 큰 Long 값의 정밀도가 손실되지 않도록 문자열로 전달합니다.
    private String id;
    private Integer sortOrder;
    private String visitDate;
    private String placeName;
    private String address;
    private String memo;

    public HomePlanItemResponseDto(PlanItem entity) {
        this.id = entity.getId().toString();
        this.sortOrder = entity.getSortOrder();
        this.visitDate = entity.getVisitDate() == null
                ? null
                : entity.getVisitDate().toString();
        this.placeName = entity.getPlaceName();
        this.address = entity.getAddress();
        this.memo = entity.getMemo();
    }
}