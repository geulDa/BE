package com.CUK.geulDa.domain.place.repository;

import com.CUK.geulDa.domain.place.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    @Query("""
        SELECT p FROM Place p
        WHERE p.isHidden = false
        AND (6371 * acos(cos(radians(:lat)) * cos(radians(p.latitude)) *
             cos(radians(p.longitude) - radians(:lon)) +
             sin(radians(:lat)) * sin(radians(p.latitude)))) <= :radiusKm
        ORDER BY p.popularityScore DESC
        """)
    List<Place> findWithinRadius(@Param("lat") double lat,
                                 @Param("lon") double lon,
                                 @Param("radiusKm") double radiusKm);

    List<Place> findByNameContainingAndIsHiddenFalse(String keyword);

    List<Place> findByIsHiddenFalse();
}