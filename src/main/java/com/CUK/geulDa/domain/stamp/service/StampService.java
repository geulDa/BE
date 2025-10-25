package com.CUK.geulDa.domain.stamp.service;

import com.CUK.geulDa.domain.member.Member;
import com.CUK.geulDa.domain.member.repository.MemberRepository;
import com.CUK.geulDa.domain.place.Place;
import com.CUK.geulDa.domain.place.repository.PlaceRepository;
import com.CUK.geulDa.domain.postcard.PostCard;
import com.CUK.geulDa.domain.postcard.UserPostCard;
import com.CUK.geulDa.domain.postcard.repository.PostCardRepository;
import com.CUK.geulDa.domain.postcard.repository.UserPostCardRepository;
import com.CUK.geulDa.domain.stamp.Stamp;
import com.CUK.geulDa.domain.stamp.dto.StampAcquireResponse;
import com.CUK.geulDa.domain.stamp.dto.StampCollectionResponse;
import com.CUK.geulDa.domain.stamp.repository.StampRepository;
import com.CUK.geulDa.global.apiReponse.code.ErrorCode;
import com.CUK.geulDa.global.apiReponse.exception.BusinessException;
import com.CUK.geulDa.global.util.GpsUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StampService {

    private final StampRepository stampRepository;
    private final PlaceRepository placeRepository;
    private final MemberRepository memberRepository;
    private final PostCardRepository postCardRepository;
    private final UserPostCardRepository userPostCardRepository;

    private static final double ACQUISITION_RADIUS_METERS = 15.0;


    public StampCollectionResponse getStampCollection(String memberId) {

        long totalStampCount = placeRepository.count();

        long collectedStampCount = stampRepository.countCompletedStampsByMemberId(memberId);

        List<String> stampIds = stampRepository.findCompletedStampIdsByMemberId(memberId);

        return StampCollectionResponse.of(totalStampCount, collectedStampCount, stampIds);
    }


    @Transactional
    public StampAcquireResponse acquireStamp(String placeId, String memberId, Double userLatitude, Double userLongitude) {

        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "ID가 " + placeId + "인 명소를 찾을 수 없습니다."
                ));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "ID가 " + memberId + "인 회원을 찾을 수 없습니다."
                ));

        boolean isWithinRadius = GpsUtils.isWithinRadius(
                userLatitude, userLongitude,
                place.getLatitude(), place.getLongitude(),
                ACQUISITION_RADIUS_METERS
        );

        if (!isWithinRadius) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "명소 15m 반경 이내에서만 획득할 수 있습니다."
            );
        }

        Stamp stamp = stampRepository.findByMemberIdAndPlaceId(memberId, placeId)
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

        PostCard postCard = postCardRepository.findByPlaceId(placeId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "해당 명소의 엽서를 찾을 수 없습니다."
                ));

        UserPostCard userPostCard = new UserPostCard(
                UUID.randomUUID().toString(),
                member,
                place,
                postCard
        );
        userPostCardRepository.save(userPostCard);

        return new StampAcquireResponse(
                stamp.getId(),
                place.getVideo(),
                place.getSystemMessage(),
                new StampAcquireResponse.PostcardInfo(
                        postCard.getImageUrl(),
                        place.getName(),
                        place.getDescription(),
                        place.getAddress()
                )
        );
    }
}