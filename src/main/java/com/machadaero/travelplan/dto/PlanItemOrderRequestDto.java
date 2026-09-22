package com.machadaero.travelplan.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

// Codex 작성: 편집 시작 시의 ID 순서와 저장할 ID 순서를 받습니다. 순번은 서버가 1부터 부여합니다.
@Getter
@Setter
public class PlanItemOrderRequestDto {
    //오래된 화면에서 저장해서 다른 변경을 덮어쓰는 일을 방지하기 위해서
    private List<Long> originalItemIds;
    private List<Long> itemIds;
}
