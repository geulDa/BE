package com.CUK.geulDa.domain.stamp.repository;

import com.CUK.geulDa.domain.stamp.Stamp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StampRepository extends JpaRepository<Stamp, Long> {

    @Query("SELECT s.id FROM Stamp s WHERE s.member.id = :memberId AND s.isCompleted = true")
    List<Long> findCompletedStampIdsByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT COUNT(s) FROM Stamp s WHERE s.member.id = :memberId AND s.isCompleted = true")
    long countCompletedStampsByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT s FROM Stamp s WHERE s.member.id = :memberId AND s.place.id = :placeId")
    Optional<Stamp> findByMemberIdAndPlaceId(@Param("memberId") Long memberId, @Param("placeId") Long placeId);
}