package com.CUK.geulDa.domain.place.repository;

import com.CUK.geulDa.domain.place.Place;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {
}