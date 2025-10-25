package com.CUK.geulDa.domain.memberEventBookmark.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import com.CUK.geulDa.domain.event.Event;
import com.CUK.geulDa.domain.event.repository.EventRepository;
import com.CUK.geulDa.domain.member.Member;
import com.CUK.geulDa.domain.member.repository.MemberRepository;
import com.CUK.geulDa.domain.memberEventBookmark.MemberEventBookmark;
import com.CUK.geulDa.domain.memberEventBookmark.repository.MemberEventBookmarkRepository;
import com.CUK.geulDa.global.apiResponse.code.ErrorCode;
import com.CUK.geulDa.global.apiResponse.code.SuccessCode;
import com.CUK.geulDa.global.apiResponse.exception.BusinessException;
import com.CUK.geulDa.global.apiResponse.response.ApiResponse;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberEventBookmarkService {

	private final MemberRepository memberRepository;
	private final EventRepository eventRepository;
	private final MemberEventBookmarkRepository bookmarkRepository;

	/**
	 * 북마크 추가
	 */
	public ApiResponse<Void> addBookmark(Member member, Long eventId) {
		Event event = eventRepository.findById(eventId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "해당 행사를 찾을 수 없습니다."));

		boolean exists = bookmarkRepository.existsByMemberIdAndEventId(member.getId(), eventId);
		if (exists) {
			throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "이미 북마크한 행사입니다.");
		}

		MemberEventBookmark bookmark = MemberEventBookmark.builder()
				.member(member)
				.event(event)
				.build();
		bookmarkRepository.save(bookmark);

		return ApiResponse.success(SuccessCode.SUCCESS_CREATE, null);
	}

	/**
	 * 북마크 삭제
	 */
	public ApiResponse<Void> removeBookmark(Member member, Long eventId) {
		boolean exists = bookmarkRepository.existsByMemberIdAndEventId(member.getId(), eventId);
		if (!exists) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "북마크되지 않은 행사입니다.");
		}

		bookmarkRepository.deleteByMemberIdAndEventId(member.getId(), eventId);
		return ApiResponse.success(SuccessCode.SUCCESS_DELETE, null);
	}

}
