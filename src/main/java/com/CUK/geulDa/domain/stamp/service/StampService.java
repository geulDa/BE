package com.CUK.geulDa.domain.stamp.service;

import com.CUK.geulDa.domain.member.Member;
import com.CUK.geulDa.domain.member.repository.MemberRepository;
import com.CUK.geulDa.domain.place.Place;
import com.CUK.geulDa.domain.place.repository.PlaceRepository;
import com.CUK.geulDa.domain.place.service.PlaceService;
import com.CUK.geulDa.domain.postcard.PostCard;
import com.CUK.geulDa.domain.postcard.UserPostCard;
import com.CUK.geulDa.domain.postcard.repository.PostCardRepository;
import com.CUK.geulDa.domain.postcard.repository.UserPostCardRepository;
import com.CUK.geulDa.domain.stamp.Stamp;
import com.CUK.geulDa.domain.stamp.dto.StampAcquireResponse;
import com.CUK.geulDa.domain.stamp.dto.StampCollectionResponse;
import com.CUK.geulDa.domain.stamp.repository.StampRepository;
import com.CUK.geulDa.global.apiResponse.code.ErrorCode;
import com.CUK.geulDa.global.apiResponse.exception.BusinessException;
import com.CUK.geulDa.global.util.GpsUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StampService {

    private final StampRepository stampRepository;
    private final PlaceRepository placeRepository;
    private final MemberRepository memberRepository;
    private final PostCardRepository postCardRepository;
    private final UserPostCardRepository userPostCardRepository;

    private static final double ACQUISITION_RADIUS_METERS = 100.0;
    private static final double HIDDEN_POSTCARD_PROBABILITY = 0.1; // 10% 확률
    private final Random random = new Random();

	public StampCollectionResponse getStampCollection(Member member) {
        long totalStampCount = placeRepository.count();

        // 비로그인 사용자: 빈 템플릿 반환
        if (member == null) {
            return StampCollectionResponse.of(totalStampCount, 0, List.of());
        }

        // 로그인 사용자: 실제 스탬프 정보 반환
        long collectedStampCount = stampRepository.countCompletedStampsByMemberId(member.getId());
        List<Long> stampIds = stampRepository.findCompletedStampIdsByMemberId(member.getId());

        return StampCollectionResponse.of(totalStampCount, collectedStampCount, stampIds);
    }

    @Transactional
    public StampAcquireResponse acquireStamp(Long placeId, Member member, Double userLatitude, Double userLongitude) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "ID가 " + placeId + "인 명소를 찾을 수 없습니다."
                ));

        boolean isWithinRadius = GpsUtils.isWithinRadius(
                userLatitude, userLongitude,
                place.getLatitude(), place.getLongitude(),
                ACQUISITION_RADIUS_METERS
        );

        if (!isWithinRadius) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "명소 100m 반경 이내에서만 획득할 수 있습니다."
            );
        }

        Stamp stamp = stampRepository.findByMemberIdAndPlaceId(member.getId(), placeId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "스탬프를 찾을 수 없습니다."
                ));

        if (stamp.getIsCompleted()) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "이미 획득한 스탬프입니다."
            );
        }

        stamp.visited();
        stampRepository.save(stamp);

        // 10% 확률로 히든 엽서 발급
        boolean isHidden = random.nextDouble() < HIDDEN_POSTCARD_PROBABILITY;

        PostCard postCard = postCardRepository.findByPlaceIdAndIsHidden(placeId, isHidden)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "해당 명소의 " + (isHidden ? "히든 " : "") + "엽서를 찾을 수 없습니다."
                ));

        UserPostCard userPostCard = UserPostCard.builder()
                .member(member)
                .place(place)
                .postcard(postCard)
                .build();
        userPostCardRepository.save(userPostCard);

        return new StampAcquireResponse(
                stamp.getId(),
                member.getId(),
                place.getVideo(),
                place.getSystemMessage(),
                new StampAcquireResponse.PostcardInfo(
                        postCard.getImageUrl(),
                        place.getName(),
                        place.getDescription(),
                        place.getAddress(),
                        isHidden
                )
        );
    }
}