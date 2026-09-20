package com.machadaero.travelplan.service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.machadaero.travelplan.dto.PlaceSearchResultDto;
import com.machadaero.travelplan.dto.PlaceDetailResponseDto;
import com.machadaero.travelplan.dto.PlaceSearchResponseDto;
import com.machadaero.travelplan.repository.TouristPlaceRepository;

@Service
public class PlaceService {

        private final TouristPlaceRepository touristPlaceRepository;

        private String SearchApi_URL = "https://apis.data.go.kr/B551011/KorService2/";

        @Value("${TOURIST_API_KEY}")
        private String touristApi_KEY;

        public PlaceService(TouristPlaceRepository touristPlaceRepository) {
                this.touristPlaceRepository = touristPlaceRepository;
        }

        // Codex 수정: 기존 매개변수 뒤에 지역·카테고리 조건을 추가합니다.
        public PlaceSearchResultDto searchPlace(
                        String keyword, int pageNo, String lDongRegnCd, String lclsSystm1) {

                System.out.println("PlaceService - searchPlace()");

                // Codex 수정: 키워드가 없거나 공백뿐인 경우에도 검색할 수 있게 처리합니다.
                keyword = keyword == null ? "" : keyword.trim();
                boolean hasKeyword = !keyword.isEmpty();

                String incodingKeyword = URLEncoder.encode(
                                keyword, StandardCharsets.UTF_8);

                // Codex 수정: 키워드 검색 API는 keyword가 필수이므로,
                // 키워드가 없으면 지역기반 목록 API를 사용합니다.
                String operation = hasKeyword ? "searchKeyword2" : "areaBasedList2";

                String url = SearchApi_URL
                                + operation
                                + "?MobileOS=WEB"
                                + "&MobileApp=Machadaero"
                                + "&numOfRows=25"
                                + "&serviceKey=" + touristApi_KEY
                                + "&pageNo=" + pageNo
                                + "&_type=json";

                // Codex 수정: 키워드가 있을 때만 요청에 추가합니다.
                if (hasKeyword) {
                        url += "&keyword=" + incodingKeyword;
                }

                // Codex 수정: '지역 전체'는 빈 값이므로 조건을 보내지 않습니다.
                if (lDongRegnCd != null && !lDongRegnCd.isBlank()) {
                        url += "&lDongRegnCd="
                                        + URLEncoder.encode(lDongRegnCd, StandardCharsets.UTF_8);
                }

                // Codex 수정: '카테고리 전체'는 빈 값이므로 조건을 보내지 않습니다.
                if (lclsSystm1 != null && !lclsSystm1.isBlank()) {
                        url += "&lclsSystm1="
                                        + URLEncoder.encode(lclsSystm1, StandardCharsets.UTF_8);
                }

                System.out.println("키워드 인코딩: " + incodingKeyword);

                RestClient touristApiClient = RestClient.create();
                Map<String, Object> result1 = touristApiClient.get()
                                .uri(URI.create(url))
                                .retrieve()
                                .body(Map.class);

                Map<String, Object> result2 = (Map<String, Object>) result1.get("response");
                Map<String, Object> result3 = (Map<String, Object>) result2.get("body");

                int totalCount = (int) result3.get("totalCount");
                System.out.println("검색된 개수" + totalCount);

                int numOfRows = (int) result3.get("numOfRows");
                System.out.println("행 개수" + numOfRows);
                System.out.println("페이지 넘버" + pageNo);

                // Codex 수정: 빈 검색 결과를 바로 반환할 수 있도록 선언 위치를 옮겼습니다.
                List<PlaceSearchResponseDto> responseDtoList = new ArrayList<>();

                // Codex 수정: 결과가 0건이면 items를 Map으로 변환하지 않습니다.
                // 빈 결과에서 items가 빈 문자열 등으로 내려오는 경우의 오류를 방지합니다.
                if (totalCount == 0) {
                        return new PlaceSearchResultDto(
                                        responseDtoList, totalCount, numOfRows, pageNo);
                }

                Map<String, Object> result4 = (Map<String, Object>) result3.get("items");
                List<Map<String, Object>> places = (List<Map<String, Object>>) result4.get("item");

                System.out.println("items: " + places);

                for (Map<String, Object> place : places) {
                        // Codex 수정: DTO 필드 순서에 맞춰 원본·썸네일 이미지 URL을 추가합니다.
                        // API에 해당 키가 없으면 null이 들어갑니다.
                        PlaceSearchResponseDto responseDto = new PlaceSearchResponseDto(
                                        (String) place.get("contentid"),
                                        (String) place.get("title"),
                                        (String) place.get("addr1"),
                                        (String) place.get("firstimage"),
                                        (String) place.get("firstimage2"));

                        System.out.println("담은 넘어온 관광지 id: " + responseDto.getContentid());

                        responseDtoList.add(responseDto);
                }

                return new PlaceSearchResultDto(
                                responseDtoList, totalCount, numOfRows, pageNo);
        }

        public PlaceDetailResponseDto searchPlaceDetail(String contentid) {
                System.out.println("PlaceService - searchPlaceDetail()");
                System.out.println("PlaceService -searchPlace()");
                String incodingContentId = URLEncoder.encode(contentid, StandardCharsets.UTF_8);

                String url = SearchApi_URL
                                + "detailCommon2"
                                + "?MobileOS=WEB"
                                + "&MobileApp=Machadaero"
                                + "&serviceKey=" + touristApi_KEY
                                + "&contentId=" + incodingContentId
                                + "&_type=json";
                System.out.println("키워드 인코딩: " + incodingContentId);

                RestClient touristApiClient = RestClient.create();
                Map<String, Object> result1 = touristApiClient.get()
                                .uri(URI.create(url))
                                .retrieve()
                                .body(Map.class);

                Map<String, Object> result2 = (Map<String, Object>) result1.get("response");
                Map<String, Object> result3 = (Map<String, Object>) result2.get("body");
                Map<String, Object> result4 = (Map<String, Object>) result3.get("items");
                List<Map<String, Object>> places = (List<Map<String, Object>>) result4.get("item");
                Map<String, Object> place = places.get(0);

                System.out.println("items: " + place);

                Object mapy = place.get("mapy");
                Object mapx = place.get("mapx");
                Double latitude = Double.valueOf(mapy.toString());
                Double longitude = Double.valueOf(mapx.toString());

                return new PlaceDetailResponseDto(
                                (String) place.get("contentid"),
                                (String) place.get("title"),
                                (String) place.get("addr1"),
                                (String) place.get("addr2"),
                                (String) place.get("overview"),
                                latitude, // 위도
                                longitude // 경도
                );

        }

}
