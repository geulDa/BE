package com.CUK.geulDa.domain.event.repository;

import com.CUK.geulDa.domain.event.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, String> {

    @Query("SELECT e FROM Event e " +
           "WHERE :targetDate BETWEEN e.startDate AND e.endDate")
    List<Event> findByDate(@Param("targetDate") LocalDate targetDate);
}
