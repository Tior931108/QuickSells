package com.example.quicksells.domain.auth.controller;

import com.example.quicksells.common.model.CommonResponse;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.auth.model.request.AuthSocialSignupRequest;
import com.example.quicksells.domain.auth.model.request.OauthTokenRequest;
import com.example.quicksells.domain.auth.model.response.AuthSocialSignupResponse;
import com.example.quicksells.domain.auth.service.OAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "소셜 로그인(Oauth) 관리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class OAuthController {

    private final OAuthService oAuthService;

    @Operation(summary = "구글 로그인 성공 데이터 반환")
    @PostMapping("/oauth/google/success")
    public ResponseEntity<CommonResponse> oauthSuccess(@RequestBody OauthTokenRequest request) {

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("구글 로그인에 성공하셨습니다.", request.getAccessToken()));
    }

    @Operation(summary = "구글 로그인 추가 정보 입력")
    @PostMapping("/oauth/google/signup")
    public ResponseEntity<CommonResponse> socialSignup(@AuthenticationPrincipal AuthUser authUser, @Valid @RequestBody AuthSocialSignupRequest request) {

        AuthSocialSignupResponse response = oAuthService.completeSocialSignup(authUser, request);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("소셜 회원가입을 성공하셨습니다.", response));
    }
}

