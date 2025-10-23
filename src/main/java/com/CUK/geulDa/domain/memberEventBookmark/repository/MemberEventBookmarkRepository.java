package com.CUK.geulDa.domain.memberEventBookmark.repository;

import com.CUK.geulDa.domain.memberEventBookmark.MemberEventBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberEventBookmarkRepository extends JpaRepository<MemberEventBookmark, String> {


    @Query("SELECT DISTINCT meb FROM MemberEventBookmark meb " +
           "JOIN FETCH meb.event " +
           "WHERE meb.member.id = :memberId")
    List<MemberEventBookmark> findByMemberIdWithEvent(@Param("memberId") String memberId);
}
