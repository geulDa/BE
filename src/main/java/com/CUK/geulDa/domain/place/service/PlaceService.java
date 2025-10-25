package com.CUK.geulDa.domain.place.service;

import com.CUK.geulDa.domain.place.Place;
import com.CUK.geulDa.domain.place.dto.PlaceDetailResponse;
import com.CUK.geulDa.domain.place.repository.PlaceRepository;
import com.CUK.geulDa.domain.postcard.UserPostCard;
import com.CUK.geulDa.domain.postcard.repository.UserPostCardRepository;
import com.CUK.geulDa.global.apiResponse.code.ErrorCode;
import com.CUK.geulDa.global.apiResponse.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final UserPostCardRepository userPostCardRepository;


    public PlaceDetailResponse getPlaceDetail(Long placeId, Long memberId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "ID가 " + placeId + "인 명소를 찾을 수 없습니다."
                ));

        Optional<UserPostCard> userPostCard = userPostCardRepository
                .findByMemberIdAndPlaceIdWithDetails(memberId, placeId);

        if (userPostCard.isPresent()) {
            UserPostCard upc = userPostCard.get();
            return PlaceDetailResponse.completed(
                    place.getId(),
                    upc.getPostcard().getImageUrl(),
                    place.getName(),
                    place.getDescription(),
                    place.getAddress()
            );
        } else {
            return PlaceDetailResponse.incomplete(place.getId());
        }
    }
}
