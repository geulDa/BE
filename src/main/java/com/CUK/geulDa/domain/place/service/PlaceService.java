package com.CUK.geulDa.domain.place.service;

import com.CUK.geulDa.domain.member.Member;
import com.CUK.geulDa.domain.place.Place;
import com.CUK.geulDa.domain.place.dto.PlaceDetailResponse;
import com.CUK.geulDa.domain.place.dto.PlaceListResponse;
import com.CUK.geulDa.domain.place.repository.PlaceRepository;
import com.CUK.geulDa.domain.postcard.UserPostCard;
import com.CUK.geulDa.domain.postcard.repository.UserPostCardRepository;
import com.CUK.geulDa.domain.stamp.repository.StampRepository;
import com.CUK.geulDa.global.apiResponse.code.ErrorCode;
import com.CUK.geulDa.global.apiResponse.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final UserPostCardRepository userPostCardRepository;
    private final StampRepository stampRepository;


    public PlaceDetailResponse getPlaceDetail(Long placeId, Member member) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "ID가 " + placeId + "인 명소를 찾을 수 없습니다."
                ));

        // 비로그인 사용자: 항상 incomplete 반환
        if (member == null) {
            return PlaceDetailResponse.incomplete(
                    place.getId(),
                    place.getPlaceImg(),
                    place.getName(),
                    place.getDescription(),
                    place.getAddress());
        }

        Optional<UserPostCard> userPostCard = userPostCardRepository
                .findByMemberIdAndPlaceIdWithDetails(member.getId(), placeId);

        if (userPostCard.isPresent()) {
            return PlaceDetailResponse.completed(
                    place.getId(),
					place.getPlaceImg(),
					place.getName(),
                    place.getDescription(),
                    place.getAddress()
            );
        } else {
			return PlaceDetailResponse.incomplete(
				place.getId(),
				place.getPlaceImg(),
				place.getName(),
				place.getDescription(),
				place.getAddress());
        }
    }

    public PlaceListResponse getPlaceList(Member member) {
        List<Place> places = placeRepository.findAll().stream()
                .sorted((p1, p2) -> Long.compare(p1.getId(), p2.getId()))
                .toList();

        // 비로그인 사용자: 스탬프 정보 없이 반환
        Set<Long> completedPlaceIdSet;
        if (member == null) {
            completedPlaceIdSet = Set.of();
        } else {
            Long memberId = member.getId();
            List<Long> completedPlaceIds = stampRepository.findCompletedPlaceIdsByMemberId(memberId);
            completedPlaceIdSet = Set.copyOf(completedPlaceIds);
        }

        List<PlaceListResponse.PlaceItem> placeItems = places.stream()
                .map(place -> PlaceListResponse.PlaceItem.of(
                        place.getId(),
                        place.getName(),
                        completedPlaceIdSet.contains(place.getId())
                ))
                .toList();

        return PlaceListResponse.of(placeItems);
    }
}
