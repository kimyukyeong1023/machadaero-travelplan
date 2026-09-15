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
import com.machadaero.travelplan.dto.SearchPlaceResponseDto;
import com.machadaero.travelplan.repository.TouristPlaceRepository;

@Service 
public class PlaceService {
    
    private final TouristPlaceRepository touristPlaceRepository;

    private String keywordSearchApi_URL="https://apis.data.go.kr/B551011/KorService2/searchKeyword2";
    
    @Value ("${TOURIST_API_KEY}")
    private  String touristApi_KEY;

    public PlaceService(TouristPlaceRepository touristPlaceRepository) {
        this.touristPlaceRepository = touristPlaceRepository;
    }


    public PlaceSearchResultDto searchPlace(String keyword,int pageNo){
        System.out.println("PlaceService -searchPlace()");
        String incodingKeyword=URLEncoder.encode(keyword,StandardCharsets.UTF_8);

        String url= keywordSearchApi_URL
                        +"?MobileOS=WEB"
                        +"&MobileApp=Machadaero"
                        +"&serviceKey="+touristApi_KEY
                        +"&keyword="+incodingKeyword
                        +"&pageNo="+pageNo
                        +"&_type=json";
        System.out.println("키워드 인코딩: "+incodingKeyword);

        RestClient touristApiClient= RestClient.create();
        Map<String,Object> result1=touristApiClient.get()
                            .uri(URI.create(url))
                            .retrieve()
                            .body(Map.class);


        
        Map<String,Object> result2= (Map<String,Object>)result1.get("response");
        Map<String,Object> result3= (Map<String,Object>)result2.get("body");

        int totalCount= (int)result3.get("totalCount");
        System.out.println("검색된 개수"+totalCount);
        int numOfRows= (int)result3.get("numOfRows");
        System.out.println("행 개수"+numOfRows);
        System.out.println("페이지 넘버"+pageNo);

        Map<String,Object> result4= (Map<String,Object>)result3.get("items");
        List<Map<String,Object>> places= (List<Map<String,Object>>)result4.get("item");
        System.out.println("items: "+places);

        List<SearchPlaceResponseDto> responseDtoList =new ArrayList<>();

        for(Map<String,Object> place: places){

            SearchPlaceResponseDto responseDto=
            new SearchPlaceResponseDto(
                (String)place.get("title"),(String)place.get("addr1"));

            responseDtoList.add(responseDto);      
            
        }
        return new PlaceSearchResultDto(responseDtoList,totalCount,numOfRows,pageNo);

                                                


    }
    
}
