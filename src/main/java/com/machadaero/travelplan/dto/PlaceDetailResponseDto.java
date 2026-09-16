package com.machadaero.travelplan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PlaceDetailResponseDto {

    private String contentid;  // 관광지 ID
    private String title;      // 관광지명
    private String addr1;      // 주소
    private String addr2;      // 상세 주소
    private String overview;   // 소개

    private Double latitude;   // 위도: 외부 API의 mapy
    private Double longitude;  // 경도: 외부 API의 mapx
    //좌표는 값이 없을 수도 있어서 기본형 double 대신 **null을 담을 수 있는 Double**로
}
