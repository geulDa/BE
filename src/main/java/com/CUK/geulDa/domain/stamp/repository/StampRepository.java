package com.CUK.geulDa.domain.stamp.repository;

import com.CUK.geulDa.domain.stamp.Stamp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StampRepository extends JpaRepository<Stamp, String> {

    @Query("SELECT s.id FROM Stamp s WHERE s.member.id = :memberId AND s.isCompleted = true")
    List<String> findCompletedStampIdsByMemberId(@Param("memberId") String memberId);

    @Query("SELECT COUNT(s) FROM Stamp s WHERE s.member.id = :memberId AND s.isCompleted = true")
    long countCompletedStampsByMemberId(@Param("memberId") String memberId);
}