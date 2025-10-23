package com.CUK.geulDa.domain.postcard.repository;

import com.CUK.geulDa.domain.postcard.PostCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostCardRepository extends JpaRepository<PostCard, String> {

    @Query("SELECT pc FROM PostCard pc " +
           "JOIN FETCH pc.place " +
           "WHERE pc.id = :postcardId")
    Optional<PostCard> findByIdWithPlace(@Param("postcardId") String postcardId);
}
