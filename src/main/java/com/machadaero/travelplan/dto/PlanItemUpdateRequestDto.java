package com.machadaero.travelplan.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlanItemUpdateRequestDto {
    private String placeName;
    private String placeAddress;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate visitDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime visitTime;

    private Integer duration;
    private String memo;
    private String extraMemo;
}
