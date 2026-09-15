package com.machadaero.travelplan.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.machadaero.travelplan.dto.PlaceSearchResultDto;
import com.machadaero.travelplan.dto.SearchPlaceResponseDto;
import com.machadaero.travelplan.service.PlaceService;


@Controller 
public class PlaceController {
    private final PlaceService placeService;

    
    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping("/")
    public String ShowMainPage() {
        System.out.println("PlaceController - ShowMainPage()");
        
        return "index";
    }
    @GetMapping("/api/places")
    public String searchPlace(@RequestParam(name="keyword", required = false) String keyword
                                ,@RequestParam (name="pageNo",defaultValue = "1") int pageNo 
                                ,Model model) {
        System.out.println("PlaceController - searchPlace()");
        System.out.println("넘어온 키워드: "+keyword);

        pageNo=  Math.max(1, pageNo );
        PlaceSearchResultDto placeSearchResultDto=placeService.searchPlace(keyword,pageNo);
        List<SearchPlaceResponseDto> searchPlaceDtoList =placeSearchResultDto.getPlaces();
        model.addAttribute("SearchPlaceResponseDto", searchPlaceDtoList);

        model.addAttribute("totalCount", placeSearchResultDto.getTotalCount());
        model.addAttribute("numOfRows", placeSearchResultDto.getNumOfRows());
        model.addAttribute("pageNo", placeSearchResultDto.getPageNo());
        model.addAttribute("keyword", keyword);
        return "index";
    }
    
    
    
}
