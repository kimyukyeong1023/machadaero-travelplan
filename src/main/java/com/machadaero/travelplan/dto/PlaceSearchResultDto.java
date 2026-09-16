package com.machadaero.travelplan.dto;

import java.util.List;

public class PlaceSearchResultDto {
    
    public PlaceSearchResultDto(List<PlaceSearchResponseDto> places, int totalCount, int numOfRows, int pageNo) {
        this.places = places;
        this.totalCount = totalCount;
        this.numOfRows = numOfRows;
        this.pageNo = pageNo;
    }
    private List<PlaceSearchResponseDto> places;
    private int totalCount;
    private int numOfRows;
    private int pageNo;

    
    public List<PlaceSearchResponseDto> getPlaces() {
        return places;
    }
    public void setPlaces(List<PlaceSearchResponseDto> places) {
        this.places = places;
    }
    public int getTotalCount() {
        return totalCount;
    }
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
    public int getNumOfRows() {
        return numOfRows;
    }
    public void setNumOfRows(int numOfRows) {
        this.numOfRows = numOfRows;
    }
    public int getPageNo() {
        return pageNo;
    }
    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }
    
}
