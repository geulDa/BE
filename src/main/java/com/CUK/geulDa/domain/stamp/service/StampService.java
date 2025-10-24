package com.CUK.geulDa.domain.stamp.service;

import com.CUK.geulDa.domain.place.repository.PlaceRepository;
import com.CUK.geulDa.domain.stamp.dto.StampCollectionResponse;
import com.CUK.geulDa.domain.stamp.repository.StampRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StampService {

    private final StampRepository stampRepository;
    private final PlaceRepository placeRepository;


    public StampCollectionResponse getStampCollection(String memberId) {

        long totalStampCount = placeRepository.count();

        long collectedStampCount = stampRepository.countCompletedStampsByMemberId(memberId);

        List<String> stampIds = stampRepository.findCompletedStampIdsByMemberId(memberId);

        return StampCollectionResponse.of(totalStampCount, collectedStampCount, stampIds);
    }
}