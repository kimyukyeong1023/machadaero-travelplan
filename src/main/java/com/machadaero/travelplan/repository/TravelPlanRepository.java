package com.machadaero.travelplan.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.machadaero.travelplan.entity.TravelPlan;

@Repository
public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long> {
    // Codex 수정: 로그인 사용자의 계획 전체를 조회합니다. 분류·정렬·페이징은 JS에서 처리합니다.
    List<TravelPlan> findAllByUser_Id(Long userId);

    // Codex 작성: 같은 계획의 순서 저장 요청이 동시에 실행되지 않도록 트랜잭션 동안 계획 행을 잠급니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from TravelPlan p where p.id = :planId")
    Optional<TravelPlan> findByIdForOrderUpdate(@Param("planId") Long planId);
}
