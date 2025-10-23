package com.CUK.geulDa.domain.memberEventBookmark.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.CUK.geulDa.domain.memberEventBookmark.service.MemberEventBookmarkService;
import com.CUK.geulDa.global.apiReponse.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/events/{eventId}/bookmark")
@RequiredArgsConstructor
@Tag(name = "Event Bookmark", description = "행사 북마크 API")
public class MemberEventBookmarkController {

	private final MemberEventBookmarkService bookmarkService;

	@Operation(summary = "행사 북마크 추가", description = "회원이 특정 행사를 북마크합니다.")
	@PostMapping
	public ResponseEntity<ApiResponse<Void>> addBookmark(
		@PathVariable String eventId,
		@RequestParam String memberId
	) {
		return ResponseEntity.ok(bookmarkService.addBookmark(memberId, eventId));
	}

	@Operation(summary = "행사 북마크 삭제", description = "회원이 특정 행사 북마크를 해제합니다.")
	@DeleteMapping
	public ResponseEntity<ApiResponse<Void>> removeBookmark(
		@PathVariable String eventId,
		@RequestParam String memberId
	) {
		return ResponseEntity.ok(bookmarkService.removeBookmark(memberId, eventId));
	}

}
