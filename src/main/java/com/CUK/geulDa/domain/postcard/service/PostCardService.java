package com.CUK.geulDa.domain.postcard.service;

import com.CUK.geulDa.domain.postcard.PostCard;
import com.CUK.geulDa.domain.postcard.dto.PostCardDetailResponse;
import com.CUK.geulDa.domain.postcard.repository.PostCardRepository;
import com.CUK.geulDa.global.apiResponse.code.ErrorCode;
import com.CUK.geulDa.global.apiResponse.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostCardService {

    private final PostCardRepository postCardRepository;

    public PostCardDetailResponse getPostCardDetail(Long postcardId) {
        PostCard postCard = postCardRepository.findByIdWithPlace(postcardId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "ID가 " + postcardId + "인 엽서를 찾을 수 없습니다."
                ));

        return new PostCardDetailResponse(postCard);
    }
}
