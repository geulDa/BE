package com.CUK.geulDa.domain.member.controller;

import com.CUK.geulDa.domain.member.dto.MemberResponse;
import com.CUK.geulDa.domain.member.dto.MypageResponse;
import com.CUK.geulDa.domain.member.service.MemberService;
import com.CUK.geulDa.global.apiResponse.code.SuccessCode;
import com.CUK.geulDa.global.apiResponse.response.ApiResponse;
import com.CUK.geulDa.global.auth.annotation.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "Member", description = "회원 API")
public class MemberController {

    private final MemberService memberService;

    @Operation(
        summary = "내 정보 조회",
        description = "현재 로그인한 사용자의 기본 정보를 조회합니다."
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMe(@CurrentMember Long memberId) {
        MemberResponse member = MemberResponse.from(memberService.getMemberById(memberId));
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.SUCCESS_READ, member));
    }

    @Operation(
        summary = "마이페이지 조회",
        description = "현재 로그인한 사용자의 프로필, 북마크한 행사, 획득한 엽서를 조회합니다."
    )
    @GetMapping("/mypage")
    public ResponseEntity<ApiResponse<MypageResponse>> getMypage(@CurrentMember Long memberId) {
        MypageResponse mypage = memberService.getMypage(memberId);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.SUCCESS_READ, mypage));
    }
}
