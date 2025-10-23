package com.CUK.geulDa.domain.memberEventBookmark.repository;

import com.CUK.geulDa.domain.memberEventBookmark.MemberEventBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberEventBookmarkRepository extends JpaRepository<MemberEventBookmark, String> {

	boolean existsByMemberIdAndEventId(String memberId, String eventId);

	void deleteByMemberIdAndEventId(String memberId, String eventId);

	Optional<MemberEventBookmark> findByMemberIdAndEventId(String memberId, String eventId);

    @Query("SELECT DISTINCT meb FROM MemberEventBookmark meb " +
           "JOIN FETCH meb.event " +
           "WHERE meb.member.id = :memberId")
    List<MemberEventBookmark> findByMemberIdWithEvent(@Param("memberId") String memberId);
}
