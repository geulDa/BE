package com.CUK.geulDa.domain.postcard.repository;

import com.CUK.geulDa.domain.postcard.UserPostCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserPostCardRepository extends JpaRepository<UserPostCard, String> {

    @Query("SELECT DISTINCT upc FROM UserPostCard upc " +
           "JOIN FETCH upc.postcard " +
           "WHERE upc.member.id = :memberId")
    List<UserPostCard> findByMemberIdWithPostCard(@Param("memberId") String memberId);
}
