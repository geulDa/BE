package com.CUK.geulDa.domain.memberEventBookmark.repository;

import com.CUK.geulDa.domain.memberEventBookmark.MemberEventBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberEventBookmarkRepository extends JpaRepository<MemberEventBookmark, Long> {

	boolean existsByMemberIdAndEventId(Long memberId, Long eventId);

	void deleteByMemberIdAndEventId(Long memberId, Long eventId);

	Optional<MemberEventBookmark> findByMemberIdAndEventId(Long memberId, Long eventId);

    @Query("SELECT DISTINCT meb FROM MemberEventBookmark meb " +
           "JOIN FETCH meb.event " +
           "WHERE meb.member.id = :memberId")
    List<MemberEventBookmark> findByMemberIdWithEvent(@Param("memberId") Long memberId);
}
