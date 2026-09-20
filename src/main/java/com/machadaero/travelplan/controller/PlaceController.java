package com.machadaero.travelplan.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.machadaero.travelplan.dto.PlaceSearchResultDto;
import com.machadaero.travelplan.dto.PlanResponseDto;
import com.machadaero.travelplan.dto.PlaceDetailResponseDto;
import com.machadaero.travelplan.dto.PlaceSearchResponseDto;
import com.machadaero.travelplan.service.PlaceService;
import com.machadaero.travelplan.service.PlanService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class PlaceController {
    private final PlaceService placeService;

    // Codex 추가: 메인 화면에서도 기존 계획 조회 서비스를 사용합니다.
    private final PlanService planService;

    // Codex 수정: PlanService도 생성자로 전달받아 보관합니다.
    public PlaceController(PlaceService placeService, PlanService planService) {
        this.placeService = placeService;
        this.planService = planService;
    }

    @GetMapping("/")
    // Codex 수정: 메인에 처음 접속할 때도 내 계획 목록을 전달합니다.
    public String ShowMainPage(HttpServletRequest request, Model model) {
        System.out.println("PlaceController - ShowMainPage()");

        addMyPlans(request, model);

        return "index";
    }

    // Codex 수정: 지역·카테고리 검색조건을 추가로 받고 화면에도 유지합니다.
    @GetMapping("/api/places")
    public String searchPlace(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(name = "lDongRegnCd", defaultValue = "") String lDongRegnCd,
            @RequestParam(name = "lclsSystm1", defaultValue = "") String lclsSystm1,
            // Codex 추가: 로그인 세션에서 사용자 ID를 확인하기 위해 받습니다.
            HttpServletRequest request,
            Model model) {

        System.out.println("PlaceController - searchPlace()");
        System.out.println("넘어온 키워드: " + keyword);
        System.out.println("넘어온 장소: " + lDongRegnCd);
        System.out.println("넘어온 카테고리: " + lclsSystm1);

        pageNo = Math.max(1, pageNo);

        // Codex 수정: 키워드 없이 검색하거나 공백만 입력한 경우 빈 문자열로 처리합니다.
        keyword = keyword == null ? "" : keyword.trim();

        // Codex 수정: 기존 검색 메서드에 지역·카테고리 조건도 전달합니다.
        PlaceSearchResultDto placeSearchResultDto = placeService.searchPlace(keyword, pageNo, lDongRegnCd, lclsSystm1);

        List<PlaceSearchResponseDto> searchPlaceDtoList = placeSearchResultDto.getPlaces();

        model.addAttribute("SearchPlaceResponseDto", searchPlaceDtoList);

        model.addAttribute("totalCount", placeSearchResultDto.getTotalCount());
        model.addAttribute("numOfRows", placeSearchResultDto.getNumOfRows());
        model.addAttribute("pageNo", placeSearchResultDto.getPageNo());
        model.addAttribute("keyword", keyword);

        // Codex 수정: 검색 후 선택값 유지와 다음 페이지 요청에 사용합니다.
        model.addAttribute("lDongRegnCd", lDongRegnCd);
        model.addAttribute("lclsSystm1", lclsSystm1);

        // Codex 추가: 관광지를 검색한 뒤에도 내 계획 목록이 표시되도록 전달합니다.
        addMyPlans(request, model);

        return "index";
    }

    @GetMapping("/api/places/{contentid}")
    @ResponseBody
    public PlaceDetailResponseDto showPlaceDetail(@PathVariable("contentid") String contentid) {
        System.out.println("PlaceController - showPlaceDetail()");
        PlaceDetailResponseDto placeDetailResponseDto = placeService.searchPlaceDetail(contentid);
        return placeDetailResponseDto;
    }

    // Codex 추가: 로그인한 사용자의 계획 목록을 메인 화면에 전달합니다.
    // 비로그인 상태에서는 조회하지 않고 빈 목록을 전달합니다.
    private void addMyPlans(HttpServletRequest request, Model model) {
        List<PlanResponseDto> plans = List.of();

        HttpSession session = request.getSession(false);

        if (session != null) {
            Long loginUserId = (Long) session.getAttribute("loginUserId");

            if (loginUserId != null) {
                plans = planService.getPlans(loginUserId);
            }
        }

        model.addAttribute("planList", plans);
    }

}
