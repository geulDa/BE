package com.CUK.geulDa.domain.event.repository;

import com.CUK.geulDa.domain.event.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, String> {

}
