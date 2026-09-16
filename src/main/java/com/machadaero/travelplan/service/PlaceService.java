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

    public PlaceSearchResultDto searchPlace(String keyword, int pageNo) {
        System.out.println("PlaceService - searchPlace()");
        System.out.println("PlaceService -searchPlace()");
        String incodingKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);

        String url = SearchApi_URL
                + "searchKeyword2"
                + "?MobileOS=WEB"
                + "&MobileApp=Machadaero"
                + "&numOfRows=5"
                + "&serviceKey=" + touristApi_KEY
                + "&keyword=" + incodingKeyword
                + "&pageNo=" + pageNo
                + "&_type=json";
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

        Map<String, Object> result4 = (Map<String, Object>) result3.get("items");
        List<Map<String, Object>> places = (List<Map<String, Object>>) result4.get("item");
        System.out.println("items: " + places);

        List<PlaceSearchResponseDto> responseDtoList = new ArrayList<>();

        for (Map<String, Object> place : places) {

            PlaceSearchResponseDto responseDto = new PlaceSearchResponseDto((String) place.get("contentid"),
                    (String) place.get("title"), (String) place.get("addr1"));
            System.out.println("담은 넘어온 관광지 id: " + responseDto.getContentid());

            responseDtoList.add(responseDto);

        }

        return new PlaceSearchResultDto(responseDtoList, totalCount, numOfRows, pageNo);

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
