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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final UserPostCardRepository userPostCardRepository;
    private final StampRepository stampRepository;


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
            return PlaceDetailResponse.incomplete(
				place.getId(),
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

    public List<String> getTourPurposeTags(Place place) {
        if (place.getTourPurposeTags() == null || place.getTourPurposeTags().isBlank()) {
            return List.of();
        }
        return List.of(place.getTourPurposeTags().split(","));
    }

    public List<Place> findPlacesWithinRadius(double lat, double lon, double radius) {
        return placeRepository.findWithinRadius(lat, lon, radius);
    }

    public List<Place> findByKeyword(String keyword) {
        return placeRepository.findByNameContainingAndIsHiddenFalse(keyword);
    }

    public List<Place> filterByPurpose(List<Place> places, String purpose) {
        // 영어 → 한글 매핑
        String koreanPurpose = switch (purpose) {
            case "dating" -> "데이트";
            case "family" -> "가족";
            case "friendship" -> "친구";
            case "foodie" -> "식도락";
            default -> purpose;
        };

        // 1. 목적에 맞는 장소 필터링
        List<Place> filtered = places.stream()
                .filter(place -> {
                    List<String> tags = getTourPurposeTags(place);
                    return tags.contains(purpose) || tags.contains(koreanPurpose);
                })
                .toList();

        // 2. 카테고리별로 그룹화
        Map<String, List<Place>> byCategory = filtered.stream()
                .collect(Collectors.groupingBy(
                        place -> place.getCategory() != null ? place.getCategory() : "기타"
                ));

        // 3. 카테고리별 우선순위 (데이트 목적 기준)
        List<String> categoryPriority = "dating".equals(purpose) || "데이트".equals(koreanPurpose)
                ? List.of("자연", "문화시설", "카페", "음식점", "쇼핑", "기타")
                : List.of("문화시설", "자연", "음식점", "카페", "쇼핑", "기타");

        // 4. 카테고리별로 가중치 랜덤 샘플링 (각 카테고리에서 최대 3개씩)
        List<Place> balanced = new ArrayList<>();
        Random random = new Random();

        for (String category : categoryPriority) {
            List<Place> categoryPlaces = byCategory.getOrDefault(category, List.of());

            // 인기도 기반 가중치 랜덤 샘플링
            List<Place> sampled = weightedRandomSample(categoryPlaces, 3, random);
            balanced.addAll(sampled);
        }

        return balanced;
    }

    /**
     * 인기도 기반 가중치 랜덤 샘플링
     * 인기도가 높을수록 선택될 확률이 높지만, 매번 다른 결과 반환
     */
    private List<Place> weightedRandomSample(List<Place> places, int count, Random random) {
        if (places.isEmpty()) {
            return List.of();
        }

        if (places.size() <= count) {
            return new ArrayList<>(places);
        }

        // 가중치 계산 (인기도를 확률로 변환)
        List<WeightedPlace> weighted = places.stream()
                .map(place -> {
                    int score = place.getPopularityScore() != null ? place.getPopularityScore() : 50;
                    // 인기도 40점 이하는 최소 가중치, 70점 이상은 높은 가중치
                    double weight = score < 40 ? 0.5 : (score >= 70 ? 3.0 : 1.0);
                    return new WeightedPlace(place, weight);
                })
                .toList();

        double totalWeight = weighted.stream().mapToDouble(w -> w.weight).sum();

        // 가중치 기반 랜덤 샘플링
        List<Place> result = new ArrayList<>();
        List<WeightedPlace> remaining = new ArrayList<>(weighted);

        for (int i = 0; i < count && !remaining.isEmpty(); i++) {
            double randomValue = random.nextDouble() * totalWeight;
            double cumulative = 0.0;

            WeightedPlace selected = null;
            for (WeightedPlace wp : remaining) {
                cumulative += wp.weight;
                if (randomValue <= cumulative) {
                    selected = wp;
                    break;
                }
            }

            if (selected == null) {
                selected = remaining.get(remaining.size() - 1);
            }

            result.add(selected.place);
            remaining.remove(selected);
            totalWeight -= selected.weight;
        }

        return result;
    }

    private record WeightedPlace(Place place, double weight) {}

    public List<Place> findByIds(List<Long> placeIds) {
        return placeRepository.findAllById(placeIds);
    }

    public List<Place> getAllVisiblePlaces() {
        return placeRepository.findByIsHiddenFalse();
    }

    /**
     * AI 생성 장소를 DB에 저장 (선택적 사용)
     */
    @Transactional
    public Place savePlace(Place place) {
        // 중복 체크 (이름 + 주소)
        List<Place> existing = placeRepository.findByNameContainingAndIsHiddenFalse(place.getName());
        for (Place existingPlace : existing) {
            if (existingPlace.getAddress() != null &&
                existingPlace.getAddress().equals(place.getAddress())) {
                return existingPlace;  // 이미 존재하면 기존 장소 반환
            }
        }

        // 새 장소 저장
        return placeRepository.save(place);
    }
}
