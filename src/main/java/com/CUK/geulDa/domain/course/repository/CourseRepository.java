package com.CUK.geulDa.domain.course.repository;

import com.CUK.geulDa.domain.course.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query("""
        SELECT c FROM Course c
        WHERE c.isHidden = false
        AND (6371 * acos(cos(radians(:lat)) * cos(radians(c.latitude)) *
             cos(radians(c.longitude) - radians(:lon)) +
             sin(radians(:lat)) * sin(radians(c.latitude)))) <= :radiusKm
        ORDER BY c.popularityScore DESC
        """)
    List<Course> findWithinRadius(@Param("lat") double lat,
                                  @Param("lon") double lon,
                                  @Param("radiusKm") double radiusKm);

    List<Course> findByNameContainingAndIsHiddenFalse(String keyword);

    List<Course> findByIsHiddenFalse();
}
