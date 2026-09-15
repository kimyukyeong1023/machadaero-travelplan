package com.machadaero.travelplan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.machadaero.travelplan.entity.TouristPlace;

@Repository 

public interface TouristPlaceRepository extends JpaRepository<TouristPlace,Long>{
    
}
