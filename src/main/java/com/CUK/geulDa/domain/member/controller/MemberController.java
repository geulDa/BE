package com.CUK.geulDa.domain.member.controller;

import com.CUK.geulDa.domain.member.dto.MypageResponse;
import com.CUK.geulDa.domain.member.service.MemberService;
import com.CUK.geulDa.global.apiReponse.code.SuccessCode;
import com.CUK.geulDa.global.apiReponse.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "Member", description = "회원 API")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "마이페이지 조회", description = "사용자의 프로필, 북마크한 행사, 획득한 엽서를 조회합니다.")
    @GetMapping("/{memberId}/mypage")
    public ResponseEntity<ApiResponse<MypageResponse>> getMypage(@PathVariable String memberId) {
        MypageResponse mypage = memberService.getMypage(memberId);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.SUCCESS_READ, mypage));
    }
}
