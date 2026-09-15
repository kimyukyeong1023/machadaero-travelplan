package com.machadaero.travelplan.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
@Table (name = "TouristPlaces")
@Entity 
public class TouristPlace {
    
    @Id()
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false ,unique = true) 
    private String apiPlaceId;
    @Column(nullable = false) 
    private String PlaceName;
    private String PlaceIntro;
    private String address;
    private Double longitude; // 경도: API의 mapx
    private Double latitude;  // 위도: API의 mapy
    @Column(nullable = false) 
    private LocalDateTime renewDate; //서비스에서 처리될때 갱신 시각을 넣어주는게 나음

    

    
}
